package com.somnus.microservice.elasticjob.dynamic.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.somnus.microservice.elasticjob.dynamic.bean.Job;
import com.somnus.microservice.elasticjob.dynamic.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
/**
 * @author somnus
 * @packageName com.somnus.microservice.elasticjob.dynamic.service
 * @title: JobService
 * @description: TODO
 * @date 2021/10/25 18:08
 */
@Slf4j
@Service
public class JobService {

    @Autowired
    private ZookeeperRegistryCenter zookeeperRegistryCenter;

    @Autowired
    private ApplicationContext ctx;

    // ????????????????????????
    private Map<String, AtomicInteger> JOB_ADD_COUNT = new ConcurrentHashMap<>();

    public void addJob(Job job) {
        // ????????????
        JobCoreConfiguration coreConfig =
                JobCoreConfiguration.newBuilder(job.getJobName(), job.getCron(), job.getShardingTotalCount())
                        .shardingItemParameters(job.getShardingItemParameters())
                        .description(job.getDescription())
                        .failover(job.isFailover())
                        .jobParameter(job.getJobParameter())
                        .misfire(job.isMisfire())
                        .jobProperties(JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), job.getJobProperties().getJobExceptionHandler())
                        .jobProperties(JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), job.getJobProperties().getExecutorServiceHandler())
                        .build();

        // ?????????????????????????????????
        LiteJobConfiguration jobConfig = null;
        JobTypeConfiguration typeConfig = null;
        String jobType = job.getJobType();
        if (jobType.equals("SIMPLE")) {
            typeConfig = new SimpleJobConfiguration(coreConfig, job.getJobClass());
        }

        if (jobType.equals("DATAFLOW")) {
            typeConfig = new DataflowJobConfiguration(coreConfig, job.getJobClass(), job.isStreamingProcess());
        }

        if (jobType.equals("SCRIPT")) {
            typeConfig = new ScriptJobConfiguration(coreConfig, job.getScriptCommandLine());
        }

        jobConfig = LiteJobConfiguration.newBuilder(typeConfig)
                .overwrite(job.isOverwrite())
                .disabled(job.isDisabled())
                .monitorPort(job.getMonitorPort())
                .monitorExecution(job.isMonitorExecution())
                .maxTimeDiffSeconds(job.getMaxTimeDiffSeconds())
                .jobShardingStrategyClass(job.getJobShardingStrategyClass())
                .reconcileIntervalMinutes(job.getReconcileIntervalMinutes())
                .build();

        List<BeanDefinition> elasticJobListeners = getTargetElasticJobListeners(job);

        // ??????SpringJobScheduler????????????????????????
        BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class);
        factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        if ("SCRIPT".equals(jobType)) {
            factory.addConstructorArgValue(null);
        } else {
            BeanDefinitionBuilder rdbFactory = BeanDefinitionBuilder.rootBeanDefinition(job.getJobClass());
            factory.addConstructorArgValue(rdbFactory.getBeanDefinition());
        }
        factory.addConstructorArgValue(zookeeperRegistryCenter);
        factory.addConstructorArgValue(jobConfig);

        // ?????????????????????????????????????????????
        if (StringUtils.hasText(job.getEventTraceRdbDataSource())) {
            BeanDefinitionBuilder rdbFactory = BeanDefinitionBuilder.rootBeanDefinition(JobEventRdbConfiguration.class);
            rdbFactory.addConstructorArgReference(job.getEventTraceRdbDataSource());
            factory.addConstructorArgValue(rdbFactory.getBeanDefinition());
        }

        factory.addConstructorArgValue(elasticJobListeners);
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory)ctx.getAutowireCapableBeanFactory();
        defaultListableBeanFactory.registerBeanDefinition("SpringJobScheduler", factory.getBeanDefinition());
        SpringJobScheduler springJobScheduler = (SpringJobScheduler) ctx.getBean("SpringJobScheduler");
        springJobScheduler.init();
        log.info("???" + job.getJobName() + "???\t" + job.getJobClass() + "\tinit success");
    }

    private List<BeanDefinition> getTargetElasticJobListeners(Job job) {
        List<BeanDefinition> result = new ManagedList<BeanDefinition>(2);
        String listeners = job.getListener();
        if (StringUtils.hasText(listeners)) {
            BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(listeners);
            factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            result.add(factory.getBeanDefinition());
        }

        String distributedListeners = job.getDistributedListener();
        long startedTimeoutMilliseconds = job.getStartedTimeoutMilliseconds();
        long completedTimeoutMilliseconds = job.getCompletedTimeoutMilliseconds();

        if (StringUtils.hasText(distributedListeners)) {
            BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(distributedListeners);
            factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            factory.addConstructorArgValue(startedTimeoutMilliseconds);
            factory.addConstructorArgValue(completedTimeoutMilliseconds);
            result.add(factory.getBeanDefinition());
        }
        return result;
    }


    public void removeJob(String jobName) throws Exception {
        CuratorFramework client = zookeeperRegistryCenter.getClient();
        client.delete().deletingChildrenIfNeeded().forPath("/" + jobName);
    }

    /**
     * ??????????????????,??????????????????????????????zk???????????????????????????????????????????????????????????????
     */
    public void monitorJobRegister() {
        CuratorFramework client = zookeeperRegistryCenter.getClient();
        @SuppressWarnings("resource")
        PathChildrenCache childrenCache = new PathChildrenCache(client, "/", true);
        PathChildrenCacheListener childrenCacheListener = new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                ChildData data = event.getData();
                switch (event.getType()) {
                    case CHILD_ADDED:
                        String config = new String(client.getData().forPath(data.getPath() + "/config"));
                        Job job = JacksonUtil.parseJson(config, Job.class);
                        // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        if (!JOB_ADD_COUNT.containsKey(job.getJobName())) {
                            JOB_ADD_COUNT.put(job.getJobName(), new AtomicInteger());
                        }
                        int count = JOB_ADD_COUNT.get(job.getJobName()).incrementAndGet();
                        if (count > 1) {
                            addJob(job);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        childrenCache.getListenable().addListener(childrenCacheListener);
        try {
            childrenCache.start(StartMode.POST_INITIALIZED_EVENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

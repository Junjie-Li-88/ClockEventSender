package com.loreal.clock;

import org.quartz.*;

public class QuartzJob implements Job {
    @Override
    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        System.out.println("Hello World i am in the firetrucking job!!");
//        System.out.println("HelloWorldJob start: " + jobContext.getFireTime());
//        JobDetail jobDetail = jobContext.getJobDetail();
//        System.out.println("Example name is: " + jobDetail.getJobDataMap().getString("example"));
//        System.out.println("HelloWorldJob end: " + jobContext.getJobRunTime() + ", key: " +            jobDetail.getKey());
//        System.out.println("HelloWorldJob next scheduled time: " + jobContext.getNextFireTime());
    }

    public void main(String[] args) {
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("myTrigger", "group1")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(5)
                        .withIntervalInSeconds(2))
                .build();
    }
}
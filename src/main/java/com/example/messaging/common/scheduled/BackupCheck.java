package com.example.messaging.common.scheduled;

import org.springframework.scheduling.TaskScheduler;

import java.util.concurrent.ScheduledFuture;

public interface BackupCheck {

	ScheduledFuture<?> scheduleBackupCheck(TaskScheduler scheduler);
}

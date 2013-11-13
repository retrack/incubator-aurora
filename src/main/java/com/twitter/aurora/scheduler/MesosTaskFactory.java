/*
 * Copyright 2013 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.aurora.scheduler;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.protobuf.ByteString;

import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;

import com.twitter.aurora.Protobufs;
import com.twitter.aurora.codec.ThriftBinaryCodec;
import com.twitter.aurora.scheduler.base.CommandUtil;
import com.twitter.aurora.scheduler.base.JobKeys;
import com.twitter.aurora.scheduler.base.SchedulerException;
import com.twitter.aurora.scheduler.base.Tasks;
import com.twitter.aurora.scheduler.configuration.Resources;
import com.twitter.aurora.scheduler.storage.entities.IAssignedTask;
import com.twitter.aurora.scheduler.storage.entities.ITaskConfig;
import com.twitter.common.quantity.Data;

import static com.google.common.base.Preconditions.checkNotNull;

import static com.twitter.common.base.MorePreconditions.checkNotBlank;

/**
 * A factory to create mesos task objects.
 */
public interface MesosTaskFactory {

  /**
   * Creates a mesos task object.
   *
   * @param task Assigned task to translate into a task object.
   * @param slaveId Id of the slave the task is being assigned to.
   * @return A new task.
   * @throws SchedulerException If the task could not be encoded.
   */
  TaskInfo createFrom(IAssignedTask task, SlaveID slaveId) throws SchedulerException;

  static class ExecutorConfig {
    private final String executorPath;

    public ExecutorConfig(String executorPath) {
      this.executorPath = checkNotBlank(executorPath);
    }

    String getExecutorPath() {
      return executorPath;
    }
  }

  static class MesosTaskFactoryImpl implements MesosTaskFactory {
    private static final Logger LOG = Logger.getLogger(MesosTaskFactoryImpl.class.getName());
    private static final String EXECUTOR_PREFIX = "thermos-";

    /**
     * Name to associate with task executors.
     */
    @VisibleForTesting
    static final String EXECUTOR_NAME = "aurora.task";

    private final String executorPath;

    @Inject
    MesosTaskFactoryImpl(ExecutorConfig executorConfig) {
      this.executorPath = executorConfig.getExecutorPath();
    }

    @VisibleForTesting
    static ExecutorID getExecutorId(String taskId) {
      return ExecutorID.newBuilder().setValue(EXECUTOR_PREFIX + taskId).build();
    }

    // TODO(wfarner): Use JobKey here.
    public static String getJobSourceName(String role, String environment, String jobName) {
      return String.format("%s.%s.%s", role, environment, jobName);
    }

    public static String getJobSourceName(ITaskConfig task) {
      return getJobSourceName(task.getOwner().getRole(), task.getEnvironment(), task.getJobName());
    }

    public static String getInstanceSourceName(ITaskConfig task, int instanceId) {
      return String.format("%s.%s", getJobSourceName(task), instanceId);
    }

    @Override
    public TaskInfo createFrom(IAssignedTask task, SlaveID slaveId) throws SchedulerException {
      checkNotNull(task);
      byte[] taskInBytes;
      try {
        taskInBytes = ThriftBinaryCodec.encode(task.newBuilder());
      } catch (ThriftBinaryCodec.CodingException e) {
        LOG.log(Level.SEVERE, "Unable to serialize task.", e);
        throw new SchedulerException("Internal error.", e);
      }

      ITaskConfig config = task.getTask();
      List<Resource> resources;
      if (task.isSetAssignedPorts()) {
        resources = Resources.from(config)
            .toResourceList(ImmutableSet.copyOf(task.getAssignedPorts().values()));
      } else {
        resources = ImmutableList.of();
      }

      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Setting task resources to "
            + Iterables.transform(resources, Protobufs.SHORT_TOSTRING));
      }
      TaskInfo.Builder taskBuilder =
          TaskInfo.newBuilder()
              .setName(JobKeys.toPath(Tasks.ASSIGNED_TO_JOB_KEY.apply(task)))
              .setTaskId(TaskID.newBuilder().setValue(task.getTaskId()))
              .setSlaveId(slaveId)
              .addAllResources(resources)
              .setData(ByteString.copyFrom(taskInBytes));

      ExecutorInfo executor = ExecutorInfo.newBuilder()
          .setCommand(CommandUtil.create(executorPath))
          .setExecutorId(getExecutorId(task.getTaskId()))
          .setName(EXECUTOR_NAME)
          .setSource(getInstanceSourceName(config, task.getInstanceId()))
          .addResources(Resources.makeMesosResource(Resources.CPUS, ResourceSlot.EXECUTOR_CPUS))
          .addResources(
              Resources.makeMesosResource(Resources.RAM_MB, ResourceSlot.EXECUTOR_RAM.as(Data.MB)))
          .build();
      return taskBuilder
          .setExecutor(executor)
          .build();
    }
  }
}

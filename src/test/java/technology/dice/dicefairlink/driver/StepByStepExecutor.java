package technology.dice.dicefairlink.driver;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StepByStepExecutor extends ScheduledThreadPoolExecutor {

  private volatile Runnable task;

  public StepByStepExecutor(int corePoolSize) {
    super(corePoolSize);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      Runnable task, long initialDelay, long period, TimeUnit unit) {
    this.task = task;
    return null;
  }

  public void step() {
    task.run();
  }
}

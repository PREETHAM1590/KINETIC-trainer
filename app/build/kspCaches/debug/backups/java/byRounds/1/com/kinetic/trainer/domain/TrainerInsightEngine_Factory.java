package com.kinetic.trainer.domain;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class TrainerInsightEngine_Factory implements Factory<TrainerInsightEngine> {
  @Override
  public TrainerInsightEngine get() {
    return newInstance();
  }

  public static TrainerInsightEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TrainerInsightEngine newInstance() {
    return new TrainerInsightEngine();
  }

  private static final class InstanceHolder {
    private static final TrainerInsightEngine_Factory INSTANCE = new TrainerInsightEngine_Factory();
  }
}

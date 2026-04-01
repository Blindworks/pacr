package com.trainingsplan.annotation;

import com.trainingsplan.entity.SubscriptionPlan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts access to a controller class or method to users with at least the
 * specified subscription plan. Enforced by {@link com.trainingsplan.aspect.SubscriptionAspect}.
 *
 * <p>Users with role {@code ADMIN} always bypass the check.
 * A PRO subscription is treated as FREE when {@code subscriptionExpiresAt} is set and has passed.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresSubscription {

    SubscriptionPlan value();
}

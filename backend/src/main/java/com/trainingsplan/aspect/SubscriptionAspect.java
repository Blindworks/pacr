package com.trainingsplan.aspect;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.security.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Enforces subscription-plan access control declared via {@link RequiresSubscription}.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Method-level annotation takes precedence over class-level annotation.</li>
 *   <li>ADMIN users always pass regardless of subscription state.</li>
 *   <li>A PRO subscription with a non-null, past {@code subscriptionExpiresAt} is downgraded to FREE.</li>
 * </ol>
 *
 * <p>If no authenticated user is present the request is rejected with 401, not 403,
 * so the caller knows authentication — not authorization — is missing.
 */
@Aspect
@Component
public class SubscriptionAspect {

    private final SecurityUtils securityUtils;

    public SubscriptionAspect(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Around("@within(com.trainingsplan.annotation.RequiresSubscription) || " +
            "@annotation(com.trainingsplan.annotation.RequiresSubscription)")
    public Object checkSubscription(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresSubscription annotation = resolveAnnotation(joinPoint);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (user.getRole() == UserRole.ADMIN) {
            return joinPoint.proceed();
        }

        SubscriptionPlan effectivePlan = effectivePlanOf(user);
        if (!satisfies(effectivePlan, annotation.value())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    annotation.value().name() + " subscription required"
            );
        }

        return joinPoint.proceed();
    }

    /**
     * Method-level annotation takes precedence over class-level annotation.
     */
    private RequiresSubscription resolveAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresSubscription methodAnnotation = signature.getMethod()
                .getAnnotation(RequiresSubscription.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return joinPoint.getTarget().getClass().getAnnotation(RequiresSubscription.class);
    }

    /**
     * Returns the user's actual active plan.
     * A PRO plan with an expiry date in the past is treated as FREE.
     */
    private SubscriptionPlan effectivePlanOf(User user) {
        SubscriptionPlan plan = user.getSubscriptionPlan();
        if (plan == SubscriptionPlan.PRO) {
            LocalDateTime expiresAt = user.getSubscriptionExpiresAt();
            if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
                return SubscriptionPlan.FREE;
            }
        }
        return plan;
    }

    /**
     * Returns true when the user's effective plan meets or exceeds the required plan.
     * Currently FREE < PRO; extend this comparison when new tiers are added.
     */
    private boolean satisfies(SubscriptionPlan effective, SubscriptionPlan required) {
        return effective.ordinal() >= required.ordinal();
    }
}

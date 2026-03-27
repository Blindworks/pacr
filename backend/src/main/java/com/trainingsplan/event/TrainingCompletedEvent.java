package com.trainingsplan.event;

import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import org.springframework.context.ApplicationEvent;

public class TrainingCompletedEvent extends ApplicationEvent {

    private final CompletedTraining completedTraining;
    private final User user;

    public TrainingCompletedEvent(Object source, CompletedTraining completedTraining, User user) {
        super(source);
        this.completedTraining = completedTraining;
        this.user = user;
    }

    public CompletedTraining getCompletedTraining() {
        return completedTraining;
    }

    public User getUser() {
        return user;
    }
}

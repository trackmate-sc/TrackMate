/*
 * Copyright 2013 Jason Winnebeck
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
package fiji.plugin.trackmate.visualization.trajeditor.plotting;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * EventHandlerManager manages a set of event handler registrations on a target, for which all of
 * those registrations can be activated or deactivated in a single step. It also keeps the handlers
 * from being added more than once.
 *
 * @author Jason Winnebeck
 */
public class EventHandlerManager {

    private final Node target;
    private final List<Registration<? extends Event>> registrations;

    public EventHandlerManager(Node target) {
        this.target = target;
        registrations = new ArrayList<Registration<? extends Event>>();
    }

    public Node getTarget() {
        return target;
    }

    /**
     * Adds an event registration, optionally adding it to the target immediately.
     */
    public <T extends Event> void addEventHandler(boolean addImmediately, EventType<T> type,
            EventHandler<? super T> handler) {
        Registration<T> reg = new Registration<T>(type, handler);
        registrations.add(reg);
        if (addImmediately) {
            target.addEventHandler(type, handler);
            reg.setRegistered(true);
        }
    }

    /**
     * Adds an event registration, adding it to the target immediately.
     */
    public <T extends Event> void addEventHandler(EventType<T> type,
            EventHandler<? super T> handler) {
        addEventHandler(true, type, handler);
    }

    /**
     * Add all currently unadded handlers (this method will not re-add).
     */
    @SuppressWarnings("unchecked")
    public void addAllHandlers() {
        for (Registration<?> registration : registrations) {
            if (!registration.isRegistered()) {
                target.addEventHandler((EventType) registration.getType(),
                        (EventHandler) registration.getHandler());
                registration.setRegistered(true);
            }
        }
    }

    /**
     * Remove all currently added handlers.
     */
    @SuppressWarnings("unchecked")
    public void removeAllHandlers() {
        for (Registration<?> registration : registrations) {
            if (registration.isRegistered()) {
                target.removeEventHandler((EventType) registration.getType(),
                        (EventHandler) registration.getHandler());
                registration.setRegistered(false);
            }
        }
    }

    private static class Registration<T extends Event> {

        private final EventType<T> type;
        private final EventHandler<? super T> handler;
        private boolean registered = false;

        public Registration(EventType<T> type, EventHandler<? super T> handler) {
            if (type == null) {
                throw new IllegalArgumentException("type cannot be null");
            }
            if (handler == null) {
                throw new IllegalArgumentException("handler cannot be null");
            }

            this.type = type;
            this.handler = handler;
        }

        public EventType<T> getType() {
            return type;
        }

        public EventHandler<? super T> getHandler() {
            return handler;
        }

        public boolean isRegistered() {
            return registered;
        }

        public void setRegistered(boolean registered) {
            this.registered = registered;
        }
    }
}

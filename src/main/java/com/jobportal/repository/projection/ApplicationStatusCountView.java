package com.jobportal.repository.projection;

import com.jobportal.domain.application.ApplicationStatus;

/** Spring Data interface projection — lets a GROUP BY query return typed rows without loading full entities. */
public interface ApplicationStatusCountView {
    ApplicationStatus getStatus();
    Long getCount();
}
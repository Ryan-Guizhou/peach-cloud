package com.peach.common;

import lombok.Data;

import java.io.Serializable;

/**
 * Request context payload.
 */
@Data
public class CurrentContextEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String language;

    private CurrentUserDO currentUserDO;
}

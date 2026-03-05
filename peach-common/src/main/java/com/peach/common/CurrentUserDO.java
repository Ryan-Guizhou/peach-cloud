package com.peach.common;

import lombok.Data;

import java.io.Serializable;

/**
 * Current user context data.
 */
@Data
public class CurrentUserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String language;
}

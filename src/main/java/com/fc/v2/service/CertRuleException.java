package com.fc.v2.service;

/**
 * 签批关策不合时抛出：人数量程、关次越界、止住/封住后仍要落笔等。
 * 消息须指名是哪一栏不合（例：人数栏不合、算齐办法为何不作数）。
 *
 * @author fuce
 * @date 2026-10-07
 */
public class CertRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CertRuleException(String message) {
        super(message);
    }
}

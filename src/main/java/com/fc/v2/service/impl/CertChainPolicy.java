package com.fc.v2.service.impl;

import java.util.Arrays;
import java.util.List;

/**
 * 签批三段关策——六问只认这一处给出的答案：
 * 这一段凭什么算齐、下一段什么时候才亮出来、被压回去时抹掉哪一段本轮的票、
 * 有一方落了不可这张单还走不走、封住之后收不收补笔、本关到几人/还差几关。
 *
 * 段次与定员都由本关策当场核：县1名先到作准、市2名先到作准（后到票沉行面累加）、
 * 省2名须同判。往后要按品类再加一道关，另起一条并列关策去担，莫回来改这一套形状。
 *
 * @author fuce
 * @date 2026-10-07
 */
final class CertChainPolicy {

    /** 票形 */
    static final int VOTE_APPROVE = 1;
    static final int VOTE_VETO = 2;

    /** 单据情形 */
    static final int STATUS_RUNNING = 0;
    static final int STATUS_SEALED = 1;
    static final int STATUS_HALTED = 2;

    /** 算齐办法：先到那一方作准（一名落「可」即齐，后到的票只沉在行面累加） */
    static final int MODE_FIRST = 0;
    /** 算齐办法：两名须同判（定员到齐且话一样，才算齐） */
    static final int MODE_UNANIMOUS = 1;

    /** 头一关、末一关关次 */
    static final int FIRST_NODE = 0;
    static final int LAST_NODE = 2;

    /** 定员量程下限/上限：写 0、写 9 都算人数这一栏不合，进不了库 */
    static final int QUOTA_MIN = 1;
    static final int QUOTA_MAX = 2;

    private CertChainPolicy() {
    }

    /** 一关一关策（关次 → 定员、算齐办法、名目） */
    static final class Node {
        final int nodeNo;
        final String nodeName;
        final int quota;
        final int mode;

        private Node(int nodeNo, String nodeName, int quota, int mode) {
            this.nodeNo = nodeNo;
            this.nodeName = nodeName;
            this.quota = quota;
            this.mode = mode;
        }

        /**
         * 本关本轮票已落齐没有——算齐办法优先，定员只作参考：
         * FIRST：头一票「可」即齐（后到的票照旧累加、不再作准）；
         * UNANIMOUS：定员数内「可」票到齐才算齐（任一「不可」在入口即已止住，到不了这里）。
         */
        boolean completedBy(int approveVotes) {
            if (mode == MODE_FIRST) {
                return approveVotes >= 1;
            }
            return approveVotes >= quota;
        }
    }

    private static final List<Node> NODES = Arrays.asList(
            new Node(0, "县级初核", 1, MODE_FIRST),
            new Node(1, "市级核认", 2, MODE_FIRST),
            new Node(2, "省局核准", 2, MODE_UNANIMOUS));

    /** 关次当场核：报文自称走到第几关，库里未必认——一律以本关策为准，越界没有这一关 */
    static Node nodeOf(Integer nodeNo) {
        if (nodeNo == null) {
            return null;
        }
        for (Node n : NODES) {
            if (n.nodeNo == nodeNo.intValue()) {
                return n;
            }
        }
        return null;
    }

    static Node firstNode() {
        return NODES.get(FIRST_NODE);
    }

    static Node lastNode() {
        return NODES.get(LAST_NODE);
    }

    /** 整张还差几关：末关关次 - 当前关次；封住后为 0 */
    static int remainingNodes(int currentNode) {
        return Math.max(0, LAST_NODE - currentNode);
    }

    /** 应签人数量程：只收 [1,2]；写 0、写 9 一律指名人数栏不合，算齐办法跟着不作数 */
    static boolean quotaInRange(Integer needCount) {
        return needCount != null && needCount >= QUOTA_MIN && needCount <= QUOTA_MAX;
    }

    static String quotaMessage() {
        return "应签人数这一栏不合：定员量程只收 " + QUOTA_MIN + ".." + QUOTA_MAX
                + "，写 0、写 9 都算这一笔不合，进不了库；人数栏既不合，本关算齐办法亦不作数";
    }
}

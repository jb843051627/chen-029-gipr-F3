package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.List;

/**
 * 签批单起意表单：只收业务四件（贴到哪一家、哪一批名录产品、当日所认的线）与各关在籍签人。
 * 段次、算齐办法、应签人数一律不从报文取——由后端关策当场数；
 * 报文若自带人数且超出定员量程（写 0、写 9），或各关名单长短与关策定员对不上，
 * 整笔拒收并指名人数栏不合。
 *
 * @author fuce
 * @date 2026-10-07
 */
public class CertBillOpenForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 一关的在籍签人名单（座次以名单次序为准） */
    public static class NodeRoster implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 关次（须落在关策三关之内，多一关少一关都不认） */
        private Integer nodeNo;
        /** 在籍签人代号（人数须与关策定员字字相符） */
        private List<String> voterNos;

        public Integer getNodeNo() {
            return nodeNo;
        }

        public void setNodeNo(Integer nodeNo) {
            this.nodeNo = nodeNo;
        }

        public List<String> getVoterNos() {
            return voterNos;
        }

        public void setVoterNos(List<String> voterNos) {
            this.voterNos = voterNos;
        }
    }

    /** 用标企业授权册代号 */
    private String holderNo;
    /** 名录产品代号 */
    private String prodNo;
    /** 起意当时认定的那一道换算贴线（点齐前钉在单上，点齐原样带进授权；不按改线后重算） */
    private String ruleCode;
    /** 单号码（可空，空则后端排；不许与在册单重） */
    private String billNo;
    /** 三关在籍签人名单（县/市/省各一关，定员由关策当场核） */
    private List<NodeRoster> rosters;

    /** 报文自称的应签人数（可空）：非空且越定员量程，整笔不合 */
    private Integer declaredNeedCount;
    /** 报文自称的算齐办法（可空）：只作参考，关策不认；人数栏不合时它跟着不作数 */
    private Integer declaredSignMode;

    public String getHolderNo() {
        return holderNo;
    }

    public void setHolderNo(String holderNo) {
        this.holderNo = holderNo;
    }

    public String getProdNo() {
        return prodNo;
    }

    public void setProdNo(String prodNo) {
        this.prodNo = prodNo;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public List<NodeRoster> getRosters() {
        return rosters;
    }

    public void setRosters(List<NodeRoster> rosters) {
        this.rosters = rosters;
    }

    public Integer getDeclaredNeedCount() {
        return declaredNeedCount;
    }

    public void setDeclaredNeedCount(Integer declaredNeedCount) {
        this.declaredNeedCount = declaredNeedCount;
    }

    public Integer getDeclaredSignMode() {
        return declaredSignMode;
    }

    public void setDeclaredSignMode(Integer declaredSignMode) {
        this.declaredSignMode = declaredSignMode;
    }
}

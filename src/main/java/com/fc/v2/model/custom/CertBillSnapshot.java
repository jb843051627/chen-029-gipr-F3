package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TGiprCertVote;

/**
 * 签批单进度快照：本关已到几人、整张还差几关、止住与点齐各算哪一态，
 * 全由落笔那一条方法当场给出，屏上只作回声，没有人手改数的口子。
 *
 * @author fuce
 * @date 2026-10-07
 */
public class CertBillSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 单据主键 */
    private Long billId;
    /** 签批单号 */
    private String billNo;
    /** 钉的企业代号 */
    private String holderNo;
    /** 钉的名录产品代号 */
    private String prodNo;
    /** 单上认定的那一道线 */
    private String ruleCode;

    /** 当前关次（后端当场核，报文自称不算） */
    private Integer currentNode;
    /** 当前关名目 */
    private String currentNodeName;
    /** 本关算齐办法（关策回声：0先到作准 1两名须同判） */
    private Integer signMode;
    /** 本关定员（关策回声：县1/市2/省2） */
    private Integer needCount;
    /** 本关本轮已到几人（当场从本轮票行数） */
    private Integer signedCount;
    /** 整张还差几关（封住为 0） */
    private Integer remainingNodes;

    /** 情形 0在签 1已点齐封住 2已止住 */
    private Integer status;
    /** 情形名目 */
    private String statusName;
    /** 已封住：补笔、涂改、追签三种动作一概不接 */
    private boolean sealed;
    /** 已止住：后头的岗不再收笔，只能压回上一关重签 */
    private boolean halted;
    /** 止住那一刻写回本关的不可原话（屏上列得出来） */
    private String vetoOpinion;

    /** 当前关本轮各票（谁落的、可不可、说了什么） */
    private List<TGiprCertVote> currentRoundVotes;

    /** 点齐带出的授权行（未点齐为空） */
    private Long grantId;
    /** 证面编号（核发当日那一个数，当场取） */
    private String grantNo;

    /** 快照时刻 */
    private Date snapshotAt;

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

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

    public Integer getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Integer currentNode) {
        this.currentNode = currentNode;
    }

    public String getCurrentNodeName() {
        return currentNodeName;
    }

    public void setCurrentNodeName(String currentNodeName) {
        this.currentNodeName = currentNodeName;
    }

    public Integer getSignMode() {
        return signMode;
    }

    public void setSignMode(Integer signMode) {
        this.signMode = signMode;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public Integer getSignedCount() {
        return signedCount;
    }

    public void setSignedCount(Integer signedCount) {
        this.signedCount = signedCount;
    }

    public Integer getRemainingNodes() {
        return remainingNodes;
    }

    public void setRemainingNodes(Integer remainingNodes) {
        this.remainingNodes = remainingNodes;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public boolean isSealed() {
        return sealed;
    }

    public void setSealed(boolean sealed) {
        this.sealed = sealed;
    }

    public boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean halted) {
        this.halted = halted;
    }

    public String getVetoOpinion() {
        return vetoOpinion;
    }

    public void setVetoOpinion(String vetoOpinion) {
        this.vetoOpinion = vetoOpinion;
    }

    public List<TGiprCertVote> getCurrentRoundVotes() {
        return currentRoundVotes;
    }

    public void setCurrentRoundVotes(List<TGiprCertVote> currentRoundVotes) {
        this.currentRoundVotes = currentRoundVotes;
    }

    public Long getGrantId() {
        return grantId;
    }

    public void setGrantId(Long grantId) {
        this.grantId = grantId;
    }

    public String getGrantNo() {
        return grantNo;
    }

    public void setGrantNo(String grantNo) {
        this.grantNo = grantNo;
    }

    public Date getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(Date snapshotAt) {
        this.snapshotAt = snapshotAt;
    }
}

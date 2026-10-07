package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 专用标志使用授权签批单对象 t_gipr_cert_bill
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_gipr_cert_bill")
@ApiModel(value = "TGiprCertBill", description = "专用标志使用授权签批单")
public class TGiprCertBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 专用标志使用授权签批单 */
    @TableField("bill_no")
    @ApiModelProperty(value = "专用标志使用授权签批单")
    private String billNo;

    /** 用标企业授权册代号（授权钉到哪一家） */
    @TableField("holder_no")
    @ApiModelProperty(value = "用标企业授权册代号（授权钉到哪一家）")
    private String holderNo;

    /** 名录产品代号（授权钉到哪一批名录产品） */
    @TableField("prod_no")
    @ApiModelProperty(value = "名录产品代号（授权钉到哪一批名录产品）")
    private String prodNo;

    /** 点齐当场认定的那一道换算贴线（钉死，不按改线后的新线重算） */
    @TableField("rule_code")
    @ApiModelProperty(value = "点齐当场认定的那一道换算贴线")
    private String ruleCode;

    /** 点齐那一刻由本单带出的授权行（一单一授，只许本单回写） */
    @TableField("grant_id")
    @ApiModelProperty(value = "点齐那一刻由本单带出的授权行")
    private Long grantId;

    /** 当前所在关 0..2（县级初核/市级核认/省局核准），一律后端当场数，报文自称不认 */
    @TableField("node_no")
    @ApiModelProperty(value = "当前所在关 0..2（县级初核/市级核认/省局核准）")
    private Integer nodeNo;

    /** 本关算齐办法 0先到作准 1两名须同判（值随后端关策走，只作回声） */
    @TableField("sign_mode")
    @ApiModelProperty(value = "本关算齐办法 0先到作准 1两名须同判")
    private Integer signMode;

    /** 本关应签人数 1..2（定员量程，关策当场数；写0/9一笔不合进不了库） */
    @TableField("need_count")
    @ApiModelProperty(value = "本关应签人数")
    private Integer needCount;

    /** 本关本轮已到人数（当场从票行数，不留手填口子） */
    @TableField("sign_count")
    @ApiModelProperty(value = "本关本轮已到人数")
    private Integer signCount;

    /** 止住那一刻写回本行的不可原话（屏上列得出来；缓过来后清空——清空也要落库，故 IGNORED） */
    @TableField(value = "veto_opinion", updateStrategy = FieldStrategy.IGNORED)
    @ApiModelProperty(value = "止住那一刻写回本行的不可原话")
    private String vetoOpinion;

    /** 签批情形 0在签 1已点齐封住 2已止住（不可） */
    @TableField("status")
    @ApiModelProperty(value = "签批情形 0在签 1已点齐封住 2已止住（不可）")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getGrantId() {
        return grantId;
    }

    public void setGrantId(Long grantId) {
        this.grantId = grantId;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
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

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public String getVetoOpinion() {
        return vetoOpinion;
    }

    public void setVetoOpinion(String vetoOpinion) {
        this.vetoOpinion = vetoOpinion;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 专用标志使用授权记录对象 t_gipr_cert_grant（只能由签批单点齐那一刻带出）
 *
 * @author fuce
 * @date 2026-10-07
 */
@TableName("t_gipr_cert_grant")
@ApiModel(value = "TGiprCertGrant", description = "专用标志使用授权记录")
public class TGiprCertGrant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 证面编号：核发当日那一个数（当场按当日顺序取，邻张拿来不认） */
    @TableField("grant_no")
    @ApiModelProperty(value = "证面编号")
    private String grantNo;

    /** 由哪一张签批单带出（一单一授） */
    @TableField("bill_id")
    @ApiModelProperty(value = "由哪一张签批单带出")
    private Long billId;

    /** 用标企业代号（点齐当场钉死） */
    @TableField("holder_no")
    @ApiModelProperty(value = "用标企业代号")
    private String holderNo;

    /** 名录产品代号（点齐当场钉死） */
    @TableField("prod_no")
    @ApiModelProperty(value = "名录产品代号")
    private String prodNo;

    /** 当时认定的那一道换算贴线（改线后重算判不符） */
    @TableField("rule_code")
    @ApiModelProperty(value = "当时认定的那一道换算贴线")
    private String ruleCode;

    /** 走完那一刻的关次（当场取自单据，字字相等） */
    @TableField("finish_node")
    @ApiModelProperty(value = "走完那一刻的关次")
    private Integer finishNode;

    /** 核发当日 */
    @TableField("issue_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "核发当日")
    private Date issueDate;

    /** 授权情形 0在册 1已撤 */
    @TableField("status")
    @ApiModelProperty(value = "授权情形 0在册 1已撤")
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

    public String getGrantNo() {
        return grantNo;
    }

    public void setGrantNo(String grantNo) {
        this.grantNo = grantNo;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
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

    public Integer getFinishNode() {
        return finishNode;
    }

    public void setFinishNode(Integer finishNode) {
        this.finishNode = finishNode;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
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

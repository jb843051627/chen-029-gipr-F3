package com.fc.v2.controller.admin;

import java.util.Date;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.mapper.auto.TGiprCertBillMapper;
import com.fc.v2.model.auto.TGiprCertBill;
import com.fc.v2.model.custom.CertBillOpenForm;
import com.fc.v2.service.CertRuleException;
import com.fc.v2.service.ITGiprCertChainService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 专用标志授权签批链 Controller（与旧签批单入口并列；旧入口形状不动）。
 * 段次、人数不由报文自带；封住的单在名册照常露脸，只是不给能动手的把手。
 *
 * @author fuce
 * @date 2026-10-07
 */
@Api(value = "专用标志授权签批链")
@Controller
@RequestMapping("/giprCertChain")
public class GiprCertChainController extends BaseController {

    @Autowired
    private ITGiprCertChainService certChainService;
    @Autowired
    private TGiprCertBillMapper certBillMapper;

    @ApiOperation(value = "签批台账跳转", notes = "签批台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("giprCertChain:view")
    public String view() {
        return "admin/giprCertChain/list";
    }

    @Log(title = "专用标志授权签批单台账", action = "list")
    @ApiOperation(value = "签批台账（封住只读的照常露脸）", notes = "签批台账")
    @GetMapping("/list")
    @RequiresPermissions("giprCertChain:list")
    @ResponseBody
    public ResultTable list() {
        startPage();
        com.github.pagehelper.PageInfo<TGiprCertBill> page =
                new com.github.pagehelper.PageInfo<TGiprCertBill>(
                        certBillMapper.selectList(new QueryWrapper<TGiprCertBill>()
                                .eq("del_flag", 0).orderByDesc("create_time")));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "签批单起意", action = "open")
    @ApiOperation(value = "起意一张签批单", notes = "段次定员后端当场数")
    @PostMapping("/open")
    @RequiresPermissions("giprCertChain:open")
    @ResponseBody
    public AjaxResult open(@RequestBody CertBillOpenForm form) {
        try {
            return AjaxResult.success("起单成功").put("data", certChainService.open(form, new Date()));
        } catch (CertRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "签批落「可」", action = "approve")
    @ApiOperation(value = "落一笔可", notes = "县一名即齐/市先到作准/省两名同判")
    @PostMapping("/approve")
    @RequiresPermissions("giprCertChain:approve")
    @ResponseBody
    public AjaxResult approve(Long id, String voterNo, String opinion) {
        try {
            return AjaxResult.success().put("data", certChainService.approve(id, voterNo, opinion, new Date()));
        } catch (CertRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "签批落「不可」", action = "veto")
    @ApiOperation(value = "落一笔不可（止住）", notes = "一票止住，话写回本关")
    @PostMapping("/veto")
    @RequiresPermissions("giprCertChain:veto")
    @ResponseBody
    public AjaxResult veto(Long id, String voterNo, String opinion) {
        try {
            return AjaxResult.success().put("data", certChainService.veto(id, voterNo, opinion, new Date()));
        } catch (CertRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "签批压回重签", action = "pressBack")
    @ApiOperation(value = "压回上一关重签", notes = "上一轮票抹掉，从头累计")
    @PostMapping("/pressBack")
    @RequiresPermissions("giprCertChain:pressBack")
    @ResponseBody
    public AjaxResult pressBack(Long id, String opinion) {
        try {
            return AjaxResult.success().put("data",
                    certChainService.pressBack(id, com.fc.v2.shiro.util.ShiroUtils.getLoginName(), opinion, new Date()));
        } catch (CertRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @ApiOperation(value = "进度快照（回声）", notes = "本关到几人/还差几关/止住点齐各态当场数")
    @GetMapping("/snapshot")
    @RequiresPermissions("giprCertChain:view")
    @ResponseBody
    public AjaxResult snapshot(Long id) {
        try {
            return AjaxResult.success().put("data", certChainService.snapshot(id, new Date()));
        } catch (CertRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @ApiOperation(value = "年底复核授权与底册", notes = "授权行回查底册行，字字相等才咬得住")
    @GetMapping("/verifyGrant")
    @RequiresPermissions("giprCertChain:verify")
    @ResponseBody
    public AjaxResult verifyGrant(Long grantId) {
        return AjaxResult.success().put("data", certChainService.verifyGrantLedger(grantId));
    }
}

package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fc.v2.mapper.auto.TGiprCertBillMapper;
import com.fc.v2.model.auto.TGiprCertBill;
import com.fc.v2.service.ITGiprCertBillService;

/**
 * 专用标志使用授权签批单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TGiprCertBillServiceImpl implements ITGiprCertBillService {

    private static final int MAX_NODE = 2;
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    /** 算齐只听本关算齐办法：0一名即齐、1两名须同判；应签人数只作参考，不进算齐口径 */
    private static final int OR_QUORUM = 1;
    private static final int AND_QUORUM = 2;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    @javax.annotation.Resource
    private TGiprCertBillMapper giprCertBillMapper;

    @Override
    public TGiprCertBill selectTGiprCertBillById(Long id) {
        return this.giprCertBillMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TGiprCertBill approve(Long id, String approver, String comment) {
        TGiprCertBill r = this.giprCertBillMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        int status = r.getStatus() == null ? STATUS_RUNNING : r.getStatus();
        if (status != STATUS_RUNNING) {
            return null;
        }
        int signed = (r.getSignCount() == null ? 0 : r.getSignCount()) + 1;
        r.setSignCount(Integer.valueOf(signed));
        // 本关算齐办法：0一名即齐，1两名须同判。算齐办法与应签人数并立时先听算齐办法，
        // 应签人数只作参考，不进算齐口径（关次与人数一律由后端当场数出来，报文说了不算）
        int mode = r.getSignMode() == null ? MODE_OR : r.getSignMode();
        boolean nodeDone = mode == MODE_AND ? signed >= AND_QUORUM : signed >= OR_QUORUM;
        if (nodeDone) {
            int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
            if (node >= MAX_NODE) {
                r.setStatus(Integer.valueOf(STATUS_PASS));
            } else {
                r.setNodeNo(Integer.valueOf(node + 1));
                r.setSignCount(Integer.valueOf(0));
                r.setStatus(Integer.valueOf(STATUS_RUNNING));
            }
        }
        appendTrace(r, approver, comment);
        this.giprCertBillMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TGiprCertBill reject(Long id, String approver, String comment) {
        TGiprCertBill r = this.giprCertBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int status = r.getStatus() == null ? STATUS_RUNNING : r.getStatus();
        if (status != STATUS_RUNNING) {
            return null;
        }
        r.setStatus(Integer.valueOf(STATUS_VETO));
        appendTrace(r, approver, comment);
        this.giprCertBillMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TGiprCertBill rollback(Long id, String comment) {
        TGiprCertBill r = this.giprCertBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 压回上一关重签（被不可止住的单子也只有这一条回头路）：本关本轮已收票一并抹掉不算，
        // 单子回到在签；重新挪到这一关时从头累计，旧票不混新票
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        r.setNodeNo(Integer.valueOf(Math.max(0, node - 1)));
        r.setSignCount(Integer.valueOf(0));
        r.setStatus(Integer.valueOf(STATUS_RUNNING));
        appendTrace(r, null, comment);
        this.giprCertBillMapper.updateById(r);
        return r;
    }

    /** 签批留痕：谁办的、说了什么，追加进备注（超长截断） */
    private static void appendTrace(TGiprCertBill r, String approver, String comment) {
        StringBuilder trace = new StringBuilder();
        if (r.getRemark() != null && !r.getRemark().isEmpty()) {
            trace.append(r.getRemark()).append(';');
        }
        if (approver != null && !approver.trim().isEmpty()) {
            trace.append(approver.trim());
        }
        if (comment != null && !comment.trim().isEmpty()) {
            trace.append(':').append(comment.trim());
        }
        String remark = trace.toString();
        if (remark.length() > 500) {
            remark = remark.substring(remark.length() - 500);
        }
        r.setRemark(remark.isEmpty() ? r.getRemark() : remark);
    }
}

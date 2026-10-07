package com.fc.v2.service;

import java.util.Date;

import com.fc.v2.model.custom.CertBillOpenForm;
import com.fc.v2.model.custom.CertBillSnapshot;

/**
 * 专用标志授权签批链 Service（与旧 ITGiprCertBillService 并列的新形状，旧路子签名不动）。
 *
 * 六问只认本接口落库那一条链给出的答案：这一段凭什么算齐、下一段什么时候才亮出来、
 * 被压回去时抹掉哪一段本轮的票、一方落不可还走不走、封住后收不收补笔、几个数各是多少。
 * 段次与人数由后端关策当场核；授权只由点齐那一刻带出，不开另起一张/徒手造证的岔路。
 *
 * @author fuce
 * @date 2026-10-07
 */
public interface ITGiprCertChainService {

    /**
     * 起意一张签批单：关次、算齐办法、定员由后端关策当场排进关籍；
     * 应签人数超出定员量程（写 0、写 9）或三关名单与定员对不上，抛 CertRuleException，
     * 指名人数这一栏不合、算齐办法为何不作数，一笔都不入库。
     * 同一企业头一张还没点齐（在签/止住），第二张起不来。
     */
    CertBillSnapshot open(CertBillOpenForm form, Date at);

    /**
     * 落一笔「可」。只认本关在籍签人；本关本轮已落过的人不得再落。
     * 县1名即齐；市2名先到那一方作准即进下一关，后到者的票照旧沉在本关行面；
     * 省2名须同判，两票齐且同是「可」才算齐。已止住/已封住一概不收笔。
     */
    CertBillSnapshot approve(Long billId, String voterNo, String opinion, Date at);

    /**
     * 落一笔「不可」：单子就此止住，后头的岗不再收笔；不可的话写回本关（屏上列得出来）。
     */
    CertBillSnapshot veto(Long billId, String voterNo, String opinion, Date at);

    /**
     * 压回上一关重签：上一轮签过的票一并抹掉不算（留痕、不计数），
     * 重新挪到这一关时从头累计；旧票混着新票数作齐即为缺陷。已封住的不得压回。
     */
    CertBillSnapshot pressBack(Long billId, String operator, String opinion, Date at);

    /**
     * 进度快照：本关已到几人、整张还差几关、止住与点齐各算哪一态，
     * 全由本方法当场数出；屏上只是回声，找不到等人手改的口子。
     */
    CertBillSnapshot snapshot(Long billId, Date at);

    /**
     * 年底复核：从授权那一行回查底册那一行与带出它的签批单，
     * 企业/产品/线/关次/证面数字字相等、底册改口回链一致才算咬住；
     * 从邻张拿来、改线之后重算的一律判不符（false）。
     */
    boolean verifyGrantLedger(Long grantId);
}

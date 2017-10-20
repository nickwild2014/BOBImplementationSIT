-- Payment Netting Unnetting amount
-- Query by Pandiarajan
-- 2017-Mar-03

SELECT *
FROM
  (SELECT trim(MAS.MASTER_REF) AS MASTER_REF,
    trim(BEV.REFNO_PFIX
    ||LPAD(BEV.REFNO_SERL,3,0))                                             AS EVENT_REF,
       sum(case  when   prod.code in ('ELD','OCI') and  pos.TRAN_CODE  in (226) then 0   
            when POS.DR_CR_FLG  = 'D'  then (POS.AMOUNT  /100)*-1 else  (POS.AMOUNT/100) end)  as AMOUNT,
    SUM(DECODE(POS.DR_CR_FLG, 'D', (POS.AMOUNT  /100)*-1, (POS.AMOUNT/100))) AS AMOUNTOLD,
    SUM(DECODE(POS.DR_CR_FLG, 'D', (POS.AMOUNT) *-1, (POS.AMOUNT)))          AS UPDAMOUNT,
    TRIM(POS.CCY)                                                            AS CURRENCY
  FROM MASTER MAS,
    BASEEVENT BEV,
    RELITEM REL,
    POSTING POS,
    PRODTYPE  prod
  WHERE MAS.KEY97    = BEV.MASTER_KEY
  AND BEV.KEY97      = REL.EVENT_KEY
  AND REL.KEY97      = POS.KEY97
  AND BEV.STATUS     = 'c'
  AND POS.POSTED_AS IS NULL
  AND POS.ACC_TYPE   = 'RTGS'
   and  MAS.PRODTYPE  =  PROD.KEY97
  GROUP BY POS.CCY ,
    trim(MAS.MASTER_REF),
    trim(BEV.REFNO_PFIX
    ||LPAD(BEV.REFNO_SERL,3,0)),
    TRIM(POS.CCY)
  UNION ALL
  SELECT trim(MAS.MASTER_REF) AS MASTER_REF,
    trim(BEV.REFNO_PFIX
    ||LPAD(BEV.REFNO_SERL,3,0)) EVENT_REF ,
     sum(case  when   prod.code in ('ELD','OCI') and  pos.TRAN_CODE  in (226) then 0   
            when POS.DR_CR_FLG  = 'D'  then (POS.AMOUNT  /100)*-1 else  (POS.AMOUNT/100) end)  as AMOUNT,
    SUM(DECODE(POS.DR_CR_FLG, 'D', (POS.AMOUNT  /100)*-1, (POS.AMOUNT/100))) AS AMOUNTOLD,
    SUM(DECODE(POS.DR_CR_FLG, 'D', (POS.AMOUNT) *-1, (POS.AMOUNT)))          AS UPDAMOUNT ,
    TRIM(POS.CCY)                                                            AS CURRENCY
  FROM MASTER MAS,
    BASEEVENT BEV,
    BASEEVENT BEV1,
    MASTER MAS1,
    RELITEM REL,
    POSTING POS,
    PRODTYPE  prod
  WHERE MAS.KEY97     = BEV.MASTER_KEY
  AND BEV.ATTACHD_EV  = BEV1.KEY97
  AND BEV1.MASTER_KEY = MAS1.KEY97
  AND bev.STATUS      = 'c'
  AND bev1.STATUS     = 'c'
  AND BEV1.KEY97      = REL.EVENT_KEY
  AND REL.KEY97       = POS.KEY97
  AND POS.ACC_TYPE    = 'RTGS'
  AND POS.POSTED_AS  IS NULL
  and  MAS.PRODTYPE  =  PROD.KEY97
  GROUP BY trim(MAS.MASTER_REF),
    trim(BEV.REFNO_PFIX
    ||LPAD(BEV.REFNO_SERL,3,0)),
    TRIM(POS.CCY)
  ) A
WHERE trim(A.MASTER_REF) = ?
AND trim(A.EVENT_REF)    = ?
 
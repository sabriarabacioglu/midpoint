CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD fullFlag BIT;
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp DATETIME(6);
ALTER TABLE m_task ADD expectedTotal BIGINT;
ALTER TABLE m_assignment ADD disableReason VARCHAR(255);
ALTER TABLE m_focus ADD disableReason VARCHAR(255);
ALTER TABLE m_shadow ADD disableReason VARCHAR(255);
ALTER TABLE m_audit_delta ADD context LONGTEXT;
ALTER TABLE m_audit_delta ADD returns LONGTEXT;
ALTER TABLE m_operation_result ADD context LONGTEXT;
ALTER TABLE m_operation_result ADD returns LONGTEXT;


CREATE INDEX iAncestorDepth ON m_org_closure (ancestor_id, ancestor_oid, depthValue);
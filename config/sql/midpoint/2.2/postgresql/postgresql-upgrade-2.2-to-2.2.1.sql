CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD COLUMN fullFlag BOOLEAN;
ALTER TABLE m_shadow ADD COLUMN fullSynchronizationTimestamp TIMESTAMP;
ALTER TABLE m_task ADD COLUMN expectedTotal INT8;
ALTER TABLE m_assignment ADD disableReason VARCHAR(255);
ALTER TABLE m_focus ADD disableReason VARCHAR(255);
ALTER TABLE m_shadow ADD disableReason VARCHAR(255);
ALTER TABLE m_audit_delta ADD context TEXT;
ALTER TABLE m_audit_delta ADD returns TEXT;
ALTER TABLE m_operation_result ADD context TEXT;
ALTER TABLE m_operation_result ADD returns TEXT;

CREATE INDEX iAncestorDepth ON m_org_closure (ancestor_id, ancestor_oid, depthValue);

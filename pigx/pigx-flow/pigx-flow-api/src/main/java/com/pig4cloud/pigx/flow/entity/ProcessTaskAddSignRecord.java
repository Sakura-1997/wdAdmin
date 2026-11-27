package com.pig4cloud.pigx.flow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pigx.common.core.util.TenantTable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 动态加签批次审计记录。
 */
@Getter
@Setter
@TenantTable
@Accessors(chain = true)
@TableName("process_task_add_sign_record")
public class ProcessTaskAddSignRecord {

	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	@TableField("flow_id")
	private String flowId;

	@TableField("process_instance_id")
	private String processInstanceId;

	@TableField("node_record_id")
	private Long nodeRecordId;

	@TableField("node_id")
	private String nodeId;

	@TableField("source_task_id")
	private String sourceTaskId;

	@TableField("added_by_user_id")
	private Long addedByUserId;

	@TableField("reason")
	private String reason;

	@TableField("target_user_ids")
	private String targetUserIds;

	@TableField("participant_user_ids_before")
	private String participantUserIdsBefore;

	@TableField("participant_user_ids_after")
	private String participantUserIdsAfter;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;

}

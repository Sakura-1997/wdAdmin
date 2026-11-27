package com.pig4cloud.pigx.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pigx.flow.entity.ProcessNodeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 流程节点记录数据访问接口
 * <p>
 * 负责流程节点记录表(wflow_process_node_record)的数据访问操作
 * 管理流程执行过程中各节点的执行记录，包括节点的开始、结束时间、执行状态、审批意见等
 * </p>
 *
 * @author cxygzl
 * @since 2023-05-10
 */
@Mapper
public interface ProcessNodeRecordMapper extends BaseMapper<ProcessNodeRecord> {

	/**
	 * 锁定当前活动节点，串行化加签与任务完成。
	 */
	@Select("""
			SELECT *
			FROM process_node_record
			WHERE process_instance_id = #{processInstanceId}
			  AND node_id = #{nodeId}
			  AND status = 1
			  AND tenant_id = #{tenantId}
			  AND del_flag = '0'
			ORDER BY id DESC
			LIMIT 1
			FOR UPDATE
			""")
	ProcessNodeRecord selectActiveForUpdate(@Param("processInstanceId") String processInstanceId,
			@Param("nodeId") String nodeId, @Param("tenantId") Long tenantId);

	/**
	 * 按任务已绑定的本轮节点记录加锁，避免误锁同节点的其他执行记录。
	 */
	@Select("""
			SELECT *
			FROM process_node_record
			WHERE id = #{nodeRecordId}
			  AND process_instance_id = #{processInstanceId}
			  AND node_id = #{nodeId}
			  AND status = 1
			  AND tenant_id = #{tenantId}
			  AND del_flag = '0'
			FOR UPDATE
			""")
	ProcessNodeRecord selectActiveByIdForUpdate(@Param("nodeRecordId") Long nodeRecordId,
			@Param("processInstanceId") String processInstanceId, @Param("nodeId") String nodeId,
			@Param("tenantId") Long tenantId);

}

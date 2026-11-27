package com.pig4cloud.pigx.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pigx.common.core.util.R;
import com.pig4cloud.pigx.flow.constant.NodeStatusEnum;
import com.pig4cloud.pigx.flow.constant.ProcessInstanceConstant;
import com.pig4cloud.pigx.flow.dto.ProcessNodeRecordAssignUserParamDto;
import com.pig4cloud.pigx.flow.entity.ProcessNodeRecord;
import com.pig4cloud.pigx.flow.entity.ProcessNodeRecordAssignUser;
import com.pig4cloud.pigx.flow.mapper.ProcessNodeRecordAssignUserMapper;
import com.pig4cloud.pigx.flow.service.IProcessNodeRecordAssignUserService;
import com.pig4cloud.pigx.flow.service.IProcessNodeRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 流程节点记录-执行人服务实现类
 * <p>
 * 该类负责管理流程节点的执行人记录，主要功能包括：
 * 1. 记录每个节点的执行人分配情况
 * 2. 跟踪执行人的任务处理状态（进行中、已完成）
 * 3. 保存执行人的审批意见和处理时间
 * 4. 处理任务完成事件，更新执行记录
 * 
 * 每个流程节点可能有多个执行人（如会签），该服务记录每个执行人的处理情况
 * </p>
 *
 * @author cxygzl
 * @since 2023-05-10
 */
@Service
@RequiredArgsConstructor
public class ProcessNodeRecordAssignUserServiceImpl
		extends ServiceImpl<ProcessNodeRecordAssignUserMapper, ProcessNodeRecordAssignUser>
		implements IProcessNodeRecordAssignUserService {

	private final IProcessNodeRecordService processNodeRecordService;

	/**
	 * 设置执行人
	 * <p>
	 * 为流程节点分配执行人，主要处理逻辑：
	 * 1. 如果是带审批意见的操作（如委托、转办），更新上一条记录的审批意见和状态
	 * 2. 创建新的执行人记录，状态为"进行中"
	 * 3. 记录任务开始时间
	 * 
	 * 该方法支持任务的转办、委托等场景
	 * </p>
	 * 
	 * @param processNodeRecordAssignUserParamDto 执行人参数，包含：
	 *                                           - taskId: 任务ID
	 *                                           - userId: 执行人ID
	 *                                           - approveDesc: 审批意见（可选）
	 *                                           - taskType: 任务类型（转办、委托等）
	 * @return R 操作结果
	 */
	@Override
	public R addAssignUser(ProcessNodeRecordAssignUserParamDto processNodeRecordAssignUserParamDto) {
		ProcessNodeRecordAssignUser previousAssignUser = this.lambdaQuery()
				.eq(ProcessNodeRecordAssignUser::getTaskId, processNodeRecordAssignUserParamDto.getTaskId())
				.orderByDesc(ProcessNodeRecordAssignUser::getId)
				.last("limit 1")
				.one();

		Long nodeRecordId = processNodeRecordAssignUserParamDto.getNodeRecordId();
		if (previousAssignUser != null) {
			nodeRecordId = previousAssignUser.getNodeRecordId();
		}
		if (nodeRecordId == null) {
			ProcessNodeRecord nodeRecord = processNodeRecordService.lambdaQuery()
					.eq(ProcessNodeRecord::getProcessInstanceId,
							processNodeRecordAssignUserParamDto.getProcessInstanceId())
					.eq(ProcessNodeRecord::getNodeId, processNodeRecordAssignUserParamDto.getNodeId())
					.eq(ProcessNodeRecord::getStatus, NodeStatusEnum.JXZ.getCode())
					.orderByDesc(ProcessNodeRecord::getId)
					.last("limit 1")
					.one();
			if (nodeRecord != null) {
				nodeRecordId = nodeRecord.getId();
			}
		}

		ProcessNodeRecordAssignUser existingParticipant = null;
		if (nodeRecordId != null) {
			existingParticipant = this.lambdaQuery()
					.eq(ProcessNodeRecordAssignUser::getNodeRecordId, nodeRecordId)
					.eq(ProcessNodeRecordAssignUser::getUserId, processNodeRecordAssignUserParamDto.getUserId())
					.one();
			if (existingParticipant != null && (previousAssignUser == null
					|| !processNodeRecordAssignUserParamDto.getUserId().equals(previousAssignUser.getUserId()))) {
				if (previousAssignUser != null
						&& StrUtil.equals("RESOLVED", processNodeRecordAssignUserParamDto.getTaskType())) {
					finishAssignment(previousAssignUser, processNodeRecordAssignUserParamDto);
					existingParticipant.setExecutionId(processNodeRecordAssignUserParamDto.getExecutionId());
					existingParticipant.setStatus(NodeStatusEnum.JXZ.getCode());
					existingParticipant.setStartTime(LocalDateTime.now());
					existingParticipant.setEndTime(null);
					existingParticipant.setApproveDesc(StrUtil.EMPTY);
					existingParticipant.setTaskType("RESOLVED");
					this.updateById(existingParticipant);
					return R.ok();
				}
				return R.failed("用户已参与当前会签");
			}
		}

		if (previousAssignUser != null
				&& previousAssignUser.getUserId().equals(processNodeRecordAssignUserParamDto.getUserId())
				&& previousAssignUser.getStatus().equals(NodeStatusEnum.JXZ.getCode())) {
			return R.ok();
		}

		String assignmentOrigin = processNodeRecordAssignUserParamDto.getAssignmentOrigin();
		Long initialUserId = processNodeRecordAssignUserParamDto.getInitialUserId();
		Long addSignRecordId = processNodeRecordAssignUserParamDto.getAddSignRecordId();
		if (previousAssignUser != null) {
			assignmentOrigin = previousAssignUser.getAssignmentOrigin();
			initialUserId = previousAssignUser.getInitialUserId();
			addSignRecordId = previousAssignUser.getAddSignRecordId();

			finishAssignment(previousAssignUser, processNodeRecordAssignUserParamDto);
		}
		assignmentOrigin = StrUtil.blankToDefault(assignmentOrigin,
				ProcessInstanceConstant.ASSIGNMENT_ORIGIN_ORIGINAL);
		initialUserId = initialUserId == null ? processNodeRecordAssignUserParamDto.getUserId() : initialUserId;

		// 创建新的执行人记录
		ProcessNodeRecordAssignUser processNodeRecordAssignUser = BeanUtil
			.copyProperties(processNodeRecordAssignUserParamDto, ProcessNodeRecordAssignUser.class);
		processNodeRecordAssignUser.setNodeRecordId(nodeRecordId);
		processNodeRecordAssignUser.setAssignmentOrigin(assignmentOrigin);
		processNodeRecordAssignUser.setInitialUserId(initialUserId);
		processNodeRecordAssignUser.setAddSignRecordId(addSignRecordId);
		processNodeRecordAssignUser.setStartTime(LocalDateTime.now());
		processNodeRecordAssignUser.setStatus(NodeStatusEnum.JXZ.getCode());  // 进行中状态
		processNodeRecordAssignUser.setApproveDesc(StrUtil.EMPTY);
		processNodeRecordAssignUser.setTaskType(ProcessInstanceConstant.ASSIGNMENT_ORIGIN_ADD_SIGN
				.equals(assignmentOrigin) ? "ADD_SIGN" : StrUtil.EMPTY);
		this.save(processNodeRecordAssignUser);

		return R.ok();
	}

	private void finishAssignment(ProcessNodeRecordAssignUser assignment,
			ProcessNodeRecordAssignUserParamDto paramDto) {
		assignment.setApproveDesc(paramDto.getApproveDesc());
		assignment.setTaskType(StrUtil.blankToDefault(paramDto.getTaskType(), "TRANSFER"));
		assignment.setStatus(NodeStatusEnum.YJS.getCode());
		assignment.setEndTime(LocalDateTime.now());
		this.updateById(assignment);
	}

	/**
	 * 任务完成通知
	 * <p>
	 * 处理任务完成事件，更新执行人记录的状态。主要功能：
	 * 1. 根据任务ID和用户ID查找进行中的执行记录
	 * 2. 更新记录状态为已完成
	 * 3. 保存审批意见、结束时间和任务数据
	 * 
	 * 该方法在任务完成时被调用，记录任务的完成信息
	 * </p>
	 * 
	 * @param processNodeRecordAssignUserParamDto 任务完成参数，包含：
	 *                                           - taskId: 任务ID
	 *                                           - userId: 执行人ID
	 *                                           - approveDesc: 审批意见
	 *                                           - data: 任务数据（JSON格式）
	 *                                           - localData: 本地任务数据
	 * @return R 操作结果
	 */
	@Override
	public R completeTaskEvent(ProcessNodeRecordAssignUserParamDto processNodeRecordAssignUserParamDto) {
		// 查找进行中的执行人记录
		ProcessNodeRecordAssignUser processNodeRecordAssignUser = this.lambdaQuery()
			.eq(ProcessNodeRecordAssignUser::getTaskId, processNodeRecordAssignUserParamDto.getTaskId())
			.eq(ProcessNodeRecordAssignUser::getUserId, processNodeRecordAssignUserParamDto.getUserId())
			.eq(ProcessNodeRecordAssignUser::getProcessInstanceId,
					processNodeRecordAssignUserParamDto.getProcessInstanceId())
			.eq(ProcessNodeRecordAssignUser::getStatus, NodeStatusEnum.JXZ.getCode())
			.orderByDesc(ProcessNodeRecordAssignUser::getId)
			.last("limit 1")
			.one();
		
		// 更新记录为已完成状态
		if (processNodeRecordAssignUser == null) {
			return R.failed("未找到活动任务执行人记录");
		}

		processNodeRecordAssignUser.setStatus(NodeStatusEnum.YJS.getCode());
		processNodeRecordAssignUser.setApproveDesc(processNodeRecordAssignUserParamDto.getApproveDesc());
		processNodeRecordAssignUser.setEndTime(LocalDateTime.now());
		processNodeRecordAssignUser.setData(processNodeRecordAssignUserParamDto.getData());
		processNodeRecordAssignUser.setLocalData(processNodeRecordAssignUserParamDto.getLocalData());
		processNodeRecordAssignUser.setTaskType("COMPLETE");
		this.updateById(processNodeRecordAssignUser);
		return R.ok();
	}

}

export type FlowUserId = string | number;

export interface TaskDetail {
	taskId?: string;
	nodeId?: string;
	name?: string;
	processName?: string;
	processInstanceId?: string;
	flowId?: string;
	canAddSign?: boolean;
	participantUserIds?: FlowUserId[];
	node?: {
		id?: string;
		[key: string]: unknown;
	};
	[key: string]: unknown;
}

export interface AddSignRequest {
	taskId: string;
	targetUserIdList: FlowUserId[];
	reason: string;
}

export interface AddSignTaskSummary {
	taskId?: string;
	userId?: FlowUserId;
	[key: string]: unknown;
}

export interface AddSignResult {
	recordId?: string | number;
	tasks?: AddSignTaskSummary[];
	[key: string]: unknown;
}

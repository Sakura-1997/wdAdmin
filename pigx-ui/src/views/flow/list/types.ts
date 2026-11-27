import type { FlowVO } from '/@/api/flow/flow/types';

export interface FlowGroupView {
	key: string;
	name: string;
	items: FlowVO[];
}

<template>
	<div class="add-node-btn-box">
		<div class="add-node-btn">
			<el-popover v-model:visible="visible" trigger="click" placement="right-start" width="268px" popper-class="flow-add-node-popper">
				<div class="add-node-popover-body">
					<button type="button" class="add-node-popover-item approver" @click="addType(1)">
						<span class="item-wrapper">
							<el-icon><User /></el-icon>
						</span>
						<span>{{ $t('flow.approver') }}</span>
					</button>
					<button type="button" class="add-node-popover-item notifier" @click="addType(2)">
						<span class="item-wrapper">
							<el-icon><Message /></el-icon>
						</span>
						<span>{{ $t('flow.carbonCopyRecipient') }}</span>
					</button>

					<button type="button" class="add-node-popover-item condition" @click="addType(4)">
						<span class="item-wrapper">
							<el-icon><Share /></el-icon>
						</span>
						<span>{{ $t('flow.conditionalBranching') }}</span>
					</button>

					<button type="button" class="add-node-popover-item parallel-gateway" @click="addType(5)">
						<span class="item-wrapper">
							<el-icon><Operation /></el-icon>
						</span>
						<span>{{ $t('flow.parallelBranch') }}</span>
					</button>
					<button type="button" class="add-node-popover-item timer" @click="addType(10)">
						<span class="item-wrapper">
							<el-icon><Timer /></el-icon>
						</span>
						<span>{{ $t('flow.timer') }}</span>
					</button>
					<button type="button" class="add-node-popover-item trigger" @click="addType(11)">
						<span class="item-wrapper">
							<el-icon><Connection /></el-icon>
						</span>
						<span>{{ $t('flow.trigger') }}</span>
					</button>
				</div>
				<template #reference>
					<button class="btn" type="button" aria-label="添加流程节点">
						<el-icon><Plus /></el-icon>
					</button>
				</template>
			</el-popover>
		</div>
	</div>
</template>
<script setup>
import { ref } from 'vue';
import other from '/@/utils/other';

const props = defineProps({
	childNodeP: {
		type: Object,
		default: () => ({}),
	},
	currentNode: {
		type: Object,
		default: () => ({}),
	},
});

const emits = defineEmits(['update:childNodeP']);
const visible = ref(false);
const addType = (type) => {
	visible.value = false;
	if (type !== 4 && type !== 5) {
		let data;
		if (type === 1) {
			data = {
				id: other.getNonDuplicateID(),
				nodeName: '审批人',
				error: true,
				type: 1,
				parentId: props.currentNode.id,
				//人员方式
				assignedType: 1,
				//单选还是多选
				multiple: false,
				//多人审批的选项
				multipleMode: 1,
				//是否允许原始审批人动态加签
				allowAddSign: false,
				//第几级部门负责人
				deptLeaderLevel: 1,
				//人员选择
				formUserId: '',
				formUserName: '',
				//表单权限
				formPerms: {},
				//审批人为空
				nobody: {
					handler: 'TO_PASS',
					assignedUser: [],
				},
				//审批人拒绝
				refuse: {
					handler: 'TO_END',
					nodeId: '',
				},
				childNode: props.childNodeP,
				nodeUserList: [],
			};
		} else if (type === 2) {
			data = {
				id: other.getNonDuplicateID(),
				parentId: props.currentNode.id,

				nodeName: '抄送人',
				type: 2,
				error: true,
				childNode: props.childNodeP,
				nodeUserList: [],
				//表单权限
				formPerms: {},
			};
		} else if (type === 10) {
			data = {
				id: other.getNonDuplicateID(),
				parentId: props.currentNode.id,
				nodeName: '延时器',
				type: 10,
				error: true,
				childNode: props.childNodeP,
				timerConfig: {
					timerType: 'DURATION',
					duration: {
						value: 30,
						unit: 'MINUTE',
					},
					dateTime: null,
					formFieldId: null,
					formFieldName: null,
				},
			};
		} else if (type === 11) {
			data = {
				id: other.getNonDuplicateID(),
				parentId: props.currentNode.id,
				nodeName: '触发器',
				type: 11,
				error: true,
				childNode: props.childNodeP,
				triggerConfig: {
					url: '',
					headers: [],
					body: [],
					returnMapping: [],
					errorHandler: 'THROW',
				},
			};
		}
		emits('update:childNodeP', data);
	} else if (type === 4) {
		const id = other.getNonDuplicateID();
		emits('update:childNodeP', {
			nodeName: '条件分支',
			type: 4,
			id: id,
			parentId: props.currentNode.id,

			childNode: props.childNodeP,
			conditionNodes: [
				{
					id: other.getNonDuplicateID(),
					nodeName: '条件1',
					groupMode: true,
					error: true,
					type: 3,
					parentId: id,

					priorityLevel: 1,
					conditionList: [
						{
							mode: true,
							conditionList: [
								{
									key: '',
									expression: '',
								},
							],
						},
					],
					nodeUserList: [],
					childNode: null,
				},
				{
					nodeName: '条件2',
					type: 3,
					id: other.getNonDuplicateID(),
					groupMode: true,
					error: false,
					parentId: id,
					priorityLevel: 2,
					conditionList: [
						{
							mode: true,
							conditionList: [
								{
									key: '',
									expression: '',
								},
							],
						},
					],
					nodeUserList: [],
					childNode: null,
				},
			],
		});
	} else if (type === 5) {
		const id = other.getNonDuplicateID();
		emits('update:childNodeP', {
			nodeName: '并行分支',
			type: 5,
			id: id,
			parentId: props.currentNode.id,

			childNode: props.childNodeP,
			conditionNodes: [
				{
					id: other.getNonDuplicateID(),
					nodeName: '分支1',
					placeHolder: '满足条件',
					parentId: id,

					error: false,
					type: 3,
					priorityLevel: 1,
					conditionList: [],
					nodeUserList: [],
					childNode: null,
				},
				{
					nodeName: '分支2',
					type: 3,
					id: other.getNonDuplicateID(),
					parentId: id,

					error: false,
					placeHolder: '满足条件',

					priorityLevel: 2,
					conditionList: [],
					nodeUserList: [],
					childNode: null,
				},
			],
		});
	}
};
</script>
<style scoped lang="css">
.add-node-btn-box {
	width: 240px;
	display: inline-flex;
	flex-shrink: 0;
	position: relative;
}

.add-node-btn-box::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: -1;
	margin: auto;
	width: 2px;
	height: 100%;
	background-color: var(--el-border-color);
}

.add-node-btn {
	user-select: none;
	width: 240px;
	padding: 20px 0 32px;
	display: flex;
	justify-content: center;
	flex-shrink: 0;
	flex-grow: 1;
}

.add-node-btn .btn {
	outline: none;
	box-shadow: var(--el-box-shadow-light);
	width: 30px;
	height: 30px;
	min-width: 30px;
	min-height: 30px;
	color: var(--el-color-white);
	background: var(--el-color-primary);
	border-radius: 50%;
	position: relative;
	border: none;
	padding: 0;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.add-node-btn .btn .iconfont {
	color: #fff;
	font-size: 16px;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 100%;
	height: 100%;
}

.add-node-btn .btn:hover {
	transform: scale(1.12);
	box-shadow: var(--el-box-shadow);
}

.add-node-btn .btn:active {
	transform: none;
	background: var(--el-color-primary-dark-2);
	box-shadow: var(--el-box-shadow-light);
}

.add-node-btn .btn:focus-visible {
	outline: 2px solid var(--el-color-primary-light-5);
	outline-offset: 3px;
}
</style>
<style scoped lang="scss">
.add-node-popover-body {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 4px;

	.add-node-popover-item {
		display: flex;
		align-items: center;
		gap: 9px;
		min-width: 0;
		padding: 9px 10px;
		border: 0;
		border-radius: var(--el-border-radius-base);
		color: var(--el-text-color-regular);
		background: transparent;
		cursor: pointer;
		font-size: 13px;
		text-align: left;
		transition: color 0.16s ease, background-color 0.16s ease;

		.item-wrapper {
			user-select: none;
			display: inline-flex;
			align-items: center;
			justify-content: center;
			width: 28px;
			height: 28px;
			flex: 0 0 28px;
			border-radius: var(--el-border-radius-base);
			background: var(--el-fill-color-light);

			.el-icon {
				font-size: 17px;
			}
		}

		&.approver {
			.item-wrapper {
				color: #ff943e;
			}
		}

		&.notifier {
			.item-wrapper {
				color: #3296fa;
			}
		}

		&.condition {
			.item-wrapper {
				color: #15bc83;
			}
		}

		&.parallel-gateway {
			.item-wrapper {
				color: var(--el-color-warning);
			}
		}

		&.timer {
			.item-wrapper {
				color: #8b5cf6;
			}
		}

		&.trigger {
			.item-wrapper {
				color: var(--el-color-primary);
			}
		}

		&:hover {
			color: var(--el-color-primary);
			background: var(--el-color-primary-light-9);
		}

		&:focus-visible {
			outline: 2px solid var(--el-color-primary-light-5);
			outline-offset: -2px;
		}
	}
}
</style>

<style>
.flow-add-node-popper.el-popper {
	padding: 8px;
}
</style>

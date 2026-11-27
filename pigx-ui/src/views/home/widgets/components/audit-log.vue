<script lang="ts">
export default {
	title: '审计日志',
	icon: 'DocumentCopy',
	description: '审计日志列表',
};
</script>
<template>
	<el-card class="box-card min-h-[280px] min-[992px]:h-96">
		<template #header>
			<div class="flex items-center justify-between">
				<span class="text-base font-semibold text-gray-800 dark:text-gray-100">{{ $t('home.auditLogsTip') }}</span>
				<el-button text size="small" class="button" @click="handleRoutr">
					{{ $t('home.moreTip') }}
					<el-icon class="ml-1"><ArrowRight /></el-icon>
				</el-button>
			</div>
		</template>
		<div class="flex flex-col gap-1.5" v-if="auditState.dataList.length > 0">
			<div v-for="(item, index) in auditState.dataList" :key="index" class="px-2.5 py-2.5 rounded-lg transition-colors hover:bg-fill-lighter dark:hover:bg-white/[0.04]">
				<div class="flex items-start gap-3">
					<div class="w-2 h-2 rounded-full bg-primary mt-1.5 flex-shrink-0"></div>
					<div class="flex-1 min-w-0">
						<p class="text-[13px] text-gray-700 dark:text-gray-300 leading-relaxed">
							<span class="text-gray-500 dark:text-gray-400">{{ item.createBy }}</span>
							{{ item.auditField }} {{ item.afterVal }} → {{ item.beforeVal }}
						</p>
					</div>
					<span class="flex-shrink-0 text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">{{ item.createTime }}</span>
				</div>
			</div>
		</div>
		<el-empty :image-size="80" v-else />
	</el-card>
</template>

<script setup lang="ts" name="SysAuditLogDashboard">
import { BasicTableProps, useTable } from '/@/hooks/table';
import { fetchList } from '/@/api/admin/audit';
import { auth } from '/@/utils/authFunction';

const router = useRouter();

// 创建基本表格参数对象
const auditState: BasicTableProps = reactive({
	createdIsNeed: auth('sys_audit_view'),
	queryForm: {},
	pageList: fetchList,
	descs: ['create_time'],
	pagination: {
		size: 4, // 每页显示数据量
	},
});

// 使用实例
useTable(auditState);

// 跳转路由
const handleRoutr = () => {
	router.push('/admin/audit/index');
};
</script>

<style scoped lang="scss">
</style>

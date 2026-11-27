/**
 * 菜单视图对象类型
 */
export interface FlowVO {
	flowId?: string;
	name?: string;
	logo?: string;
	remark?: string;
	rangeShow?: string;
	stop?: boolean;
	updated?: string;

	/**
	 * 菜单ID
	 */
	formId?: string;
	/**
	 * 菜单名称
	 */
	formName?: string;
}

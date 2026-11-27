/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pigx.admin.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.pig4cloud.pigx.admin.api.constant.AnalyticsFetchStatus;
import com.pig4cloud.pigx.common.core.util.TenantTable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 站点统计缓存
 *
 * @author lengleng
 * @date 2026-06-24
 */
@Data
@TenantTable
@Schema(description = "站点统计缓存")
@EqualsAndHashCode(callSuper = true)
public class SysAnalyticsData extends Model<SysAnalyticsData> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "统计来源：clarity 或 umami")
	private String provider;

	@Schema(description = "数据日期")
	private LocalDate dataDate;

	@Schema(description = "统计天数")
	private Integer numOfDays;

	@Schema(description = "数据拉取状态：pending=占位中, success=成功, failed=失败")
	private AnalyticsFetchStatus fetchStatus;

	@Schema(description = "统计展示快照 JSON")
	private String dataJson;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记，1:已删除，0:正常")
	private String delFlag;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

}

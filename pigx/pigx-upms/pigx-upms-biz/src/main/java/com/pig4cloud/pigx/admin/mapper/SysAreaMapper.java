package com.pig4cloud.pigx.admin.mapper;

import com.pig4cloud.pigx.admin.api.entity.SysAreaEntity;
import com.pig4cloud.pigx.admin.api.vo.SysAreaChildCountVO;
import com.pig4cloud.pigx.common.data.datascope.PigxBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SysAreaMapper extends PigxBaseMapper<SysAreaEntity> {

	/**
	 * 按父级行政编码统计直属子级数量
	 *
	 * @param parentCodes 父级行政编码
	 * @return 子级数量
	 */
	List<SysAreaChildCountVO> selectChildCounts(@Param("parentCodes") Collection<Long> parentCodes);

}

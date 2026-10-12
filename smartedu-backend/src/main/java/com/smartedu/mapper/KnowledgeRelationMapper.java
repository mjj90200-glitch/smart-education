package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.KnowledgeRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识点关联 Mapper 接口
 * @author SmartEdu Team
 */
@Mapper
public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelation> {

}

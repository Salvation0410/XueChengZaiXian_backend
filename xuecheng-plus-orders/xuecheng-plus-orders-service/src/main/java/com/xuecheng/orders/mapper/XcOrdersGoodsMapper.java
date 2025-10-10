package com.xuecheng.orders.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface XcOrdersGoodsMapper extends BaseMapper<XcOrdersGoods> {

    int batchInsert(@Param("list")List<XcOrdersGoods> goodsList);

}

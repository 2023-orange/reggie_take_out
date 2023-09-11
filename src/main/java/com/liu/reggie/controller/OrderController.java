package com.liu.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liu.reggie.common.BaseContext;
import com.liu.reggie.common.R;
import com.liu.reggie.dto.OrdersDto;
import com.liu.reggie.entity.OrderDetail;
import com.liu.reggie.entity.Orders;
import com.liu.reggie.service.OrderDetailService;
import com.liu.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;


    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    @GetMapping("/userPage")
    public R<Page> userPage(int page,int pageSize){
        //获取当前id
        Long id = BaseContext.getCurrentId();
        //构造分页构造器
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //查询当前用户id订单数据
        queryWrapper.eq(id != null,Orders::getUserId,id);
        //按时间降序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo,queryWrapper);

        List<OrdersDto> list = pageInfo.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            //获取orderId,然后根据这个id，去orderDetail表中查数据
            Long itemId = item.getId();
            LambdaQueryWrapper<OrderDetail> QueryWrapper = new LambdaQueryWrapper<>();
            QueryWrapper.eq(OrderDetail::getId,itemId);
            List<OrderDetail> detailList = orderDetailService.list(QueryWrapper);
            BeanUtils.copyProperties(item,ordersDto);
            ordersDto.setOrderDetails(detailList);
            return ordersDto;
        }).collect(Collectors.toList());
        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
        ordersDtoPage.setRecords(list);
        //日志输出看一下
        log.info("list:{}", list);
        return R.success(ordersDtoPage);
    }

    /**
     * 后台订单明细
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
//    @GetMapping("/page")
//    public R<Page> page(int page,int pageSize,Long number,String beginTime,String endTime){
//        log.info("page:{},pageSize:{},number:{},beginTime:{},endTime:{}",page,pageSize,number,beginTime,endTime);
//        Page<Orders> pageInfo = new Page<>(page, pageSize);
//        Page<OrdersDto> ordersDtoPage = new Page<>(page, pageSize);
//        //条件构造器
//        LambdaQueryWrapper<Orders> queryWrapper= new LambdaQueryWrapper<>();
//        //按时间降序排序
//        queryWrapper.orderByDesc(Orders::getOrderTime);
//        //订单号
//        queryWrapper.eq(number != null,Orders::getId,number);
//        //时间段大于开始，小于结束
//        queryWrapper.gt(!(StringUtils.isNotEmpty(beginTime)),Orders::getOrderTime, beginTime).lt
//                (!StringUtils.isEmpty(endTime), Orders::getOrderTime, endTime);
//        orderService.page(pageInfo,queryWrapper);
//        List<OrdersDto> list = pageInfo.getRecords().stream().map((item) -> {
//            OrdersDto ordersDto = new OrdersDto();
//            //获取orderId,然后根据这个id，去orderDetail表中查数据
//            Long orderId = item.getId();
//            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
//            wrapper.eq(OrderDetail::getOrderId,orderId);
//            List<OrderDetail> orderDetailList = orderDetailService.list(wrapper);
//            BeanUtils.copyProperties(item,ordersDto);
//            //set一下 List<OrderDetail> orderDetailList
//            ordersDto.setOrderDetails(orderDetailList);
//            return ordersDto;
//        }).collect(Collectors.toList());
//        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
//        ordersDtoPage.setRecords(list);
//        //日志输出看一下
//        log.info("list:{}", list);
//        return R.success(ordersDtoPage);
//    }
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long number, String beginTime, String endTime) {
        //获取当前id
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //按时间降序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //订单号
        queryWrapper.eq(number != null, Orders::getId, number);
        //时间段，大于开始，小于结束
        queryWrapper.gt(!StringUtils.isEmpty(beginTime), Orders::getOrderTime, beginTime)
                .lt(!StringUtils.isEmpty(endTime), Orders::getOrderTime, endTime);
        orderService.page(pageInfo, queryWrapper);
        List<OrdersDto> list = pageInfo.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            //获取orderId,然后根据这个id，去orderDetail表中查数据
            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> details = orderDetailService.list(wrapper);
            BeanUtils.copyProperties(item, ordersDto);
            //之后set一下属性
            ordersDto.setOrderDetails(details);
            return ordersDto;
        }).collect(Collectors.toList());
        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
        ordersDtoPage.setRecords(list);
        //日志输出看一下
        log.info("list:{}", list);
        return R.success(ordersDtoPage);
    }

    /**
     * 后台修改订单状态
     * @param map
     * @return
     */
    @PutMapping
    public R<String> updateStatus(@RequestBody Map<String,String> map){
        //将一个字符串类型的变量转换为整数类型，并将结果存储在名为status的整数变量中。
        int status = Integer.parseInt(map.get("status"));
        Long orderId = Long.valueOf(map.get("id"));
        log.info("修改订单状态:status={status},id={id}", status, orderId);
        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Orders::getId,orderId);
        updateWrapper.set(Orders::getStatus,status);
        orderService.update(updateWrapper);
        return R.success("订单状态修改成功");
    }
}

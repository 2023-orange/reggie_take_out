package com.liu.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liu.reggie.entity.OrderDetail;
import com.liu.reggie.mapper.OrderDetailMapper;
import com.liu.reggie.service.OrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper,OrderDetail> implements OrderDetailService {
}

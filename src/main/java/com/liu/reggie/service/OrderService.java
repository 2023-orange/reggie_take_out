package com.liu.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liu.reggie.entity.Orders;

public interface OrderService extends IService<Orders> {
    void submit(Orders orders);
}

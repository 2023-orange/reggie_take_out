package com.liu.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liu.reggie.entity.DishFlavor;
import com.liu.reggie.mapper.DishFlavorMapper;
import com.liu.reggie.service.DishFlavorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper, DishFlavor> implements DishFlavorService {
}

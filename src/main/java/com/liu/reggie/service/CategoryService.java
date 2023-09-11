package com.liu.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liu.reggie.entity.Category;


public interface CategoryService extends IService<Category> {
    public void remove(Long id);
}

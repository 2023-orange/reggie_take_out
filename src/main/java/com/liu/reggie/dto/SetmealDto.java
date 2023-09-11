package com.liu.reggie.dto;

import com.liu.reggie.entity.Setmeal;
import com.liu.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}

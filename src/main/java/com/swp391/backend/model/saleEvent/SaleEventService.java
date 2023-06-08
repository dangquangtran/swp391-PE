package com.swp391.backend.model.saleEvent;

import com.swp391.backend.model.product.ProductService;
import com.swp391.backend.model.productSale.ProductSale;
import com.swp391.backend.model.productSale.ProductSaleKey;
import com.swp391.backend.model.productSale.ProductSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleEventService {
    private final SaleEventRepository saleEventRepository;
    private final ProductService productService;
    private final ProductSaleService productSaleService;

    public List<SaleEvent> getAll() {
        return saleEventRepository.findAll();
    }

    public SaleEvent getSaleEventById(Integer id) {
        return saleEventRepository.findById(id).orElse(null);
    }

    public SaleEvent save(SaleEvent saleEvent) {
        return saleEventRepository.save(saleEvent);
    }

    public void delete(Integer id) {
        saleEventRepository.deleteById(id);
    }

    public void init() {
        SaleEvent event = new SaleEvent();
        event.setName("Flash Sale");
        event.setStart(new Date());
        event.setEndding(new Date());
        event.setPercent(10);
        save(event);

        for(int i = 1; i <= 4; i++)
        {
            ProductSale ps = new ProductSale();
            ProductSaleKey pk = new ProductSaleKey();
            pk.setProductId(i);
            pk.setSaleEventId(event.getId());

            ps.setProduct(productService.getProductById(i));
            ps.setSaleEvent(event);
            ps.setId(pk);
            ps.setSaleQuantity(10);
            ps.setSold(3);
            productSaleService.save(ps);
        }

        for(int i = 20; i <= 21; i++)
        {
            ProductSale ps = new ProductSale();
            ProductSaleKey pk = new ProductSaleKey();
            pk.setProductId(i);
            pk.setSaleEventId(event.getId());

            ps.setProduct(productService.getProductById(i));
            ps.setSaleEvent(event);
            ps.setId(pk);
            ps.setSaleQuantity(10);
            ps.setSold(2);
            productSaleService.save(ps);
        }

        for(int i = 40; i <= 41; i++)
        {
            ProductSale ps = new ProductSale();
            ProductSaleKey pk = new ProductSaleKey();
            pk.setProductId(i);
            pk.setSaleEventId(event.getId());

            ps.setProduct(productService.getProductById(i));
            ps.setSaleEvent(event);
            ps.setId(pk);
            ps.setSaleQuantity(10);
            ps.setSold(4);
            productSaleService.save(ps);
        }

        for(int i = 60; i <= 61; i++)
        {
            ProductSale ps = new ProductSale();
            ProductSaleKey pk = new ProductSaleKey();
            pk.setProductId(i);
            pk.setSaleEventId(event.getId());

            ps.setProduct(productService.getProductById(i));
            ps.setSaleEvent(event);
            ps.setId(pk);
            ps.setSaleQuantity(10);
            ps.setSold(6);
            productSaleService.save(ps);
        }
    }
}

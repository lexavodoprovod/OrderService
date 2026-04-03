package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.ItemExistException;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.exception.ItemNullParameterException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.repository.ItemDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemDao itemDao;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemServiceImpl itemService;


    @Nested
    @DisplayName("Create Item Tests")
    class CreateItemTests {

        @Test
        @DisplayName("Should create item successfully")
        void shouldCreateItemSuccessfully() {
            ItemDto itemDto = new ItemDto(1L, "hammer", 1000L);
            Item item = Item.builder()
                    .id(1L)
                    .name("hammer")
                    .price(1000L)
                    .build();


        when(itemDao.existsByName(anyString())).thenReturn(false);
        when(itemMapper.toItem(itemDto)).thenReturn(item);
        when(itemDao.save(item)).thenReturn(item);
        when(itemMapper.toItemDto(item)).thenReturn(itemDto);


        ItemDto createItemDto = itemService.createItem(itemDto);

        assertNotNull(createItemDto);
        assertEquals(itemDto.getName(), createItemDto.getName());
        verify(itemDao, times(1)).existsByName(anyString());
        verify(itemMapper, times(1)).toItem(itemDto);
        verify(itemDao, times(1)).save(item);
        verify(itemMapper, times(1)).toItemDto(item);
        }





            @Test
            @DisplayName("Should throw ItemNullParameterException when dto is null")
            void shouldThrowExceptionWhenItemDtoIsNull() {
                assertThrows(ItemNullParameterException.class, () -> itemService.createItem(null));

                verifyNoInteractions(itemDao, itemMapper);
            }

            @Test
            @DisplayName("Should throw ItemExistException when name already exists")
            void shouldThrowExceptionWhenItemNameExists() {
                String itemName = "drill";
                ItemDto itemDto = new ItemDto(null, itemName, 500L);

                when(itemDao.existsByName(itemName)).thenReturn(true);

                ItemExistException exception = assertThrows(ItemExistException.class,
                        () -> itemService.createItem(itemDto));

                assertTrue(exception.getMessage().contains(itemName));

                verify(itemDao).existsByName(itemName);
                verify(itemDao, never()).save(any());
                verifyNoInteractions(itemMapper);

        }

    }

    @Nested
    @DisplayName("Get Item By Id Tests")
    class GetItemByIdTests {

        @Test
        @DisplayName("Should return ItemDto when item exists")
        void shouldReturnItemDtoWhenIdExists() {
            Long itemId = 1L;
            Item item = Item.builder()
                    .id(itemId)
                    .name("Saw")
                    .price(500L)
                    .build();
            ItemDto expectedDto = new ItemDto(itemId, "Saw", 500L);

            when(itemDao.findById(itemId)).thenReturn(Optional.of(item));
            when(itemMapper.toItemDto(item)).thenReturn(expectedDto);

            ItemDto result = itemService.getItemById(itemId);

            assertNotNull(result);
            assertEquals(expectedDto.getName(), result.getName());
            assertEquals(itemId, result.getId());

            verify(itemDao, times(1)).findById(itemId);
            verify(itemMapper, times(1)).toItemDto(item);
        }

        @Test
        @DisplayName("Should throw ItemNullParameterException when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(ItemNullParameterException.class, () -> itemService.getItemById(null));

            verifyNoInteractions(itemDao, itemMapper);
        }

        @Test
        @DisplayName("Should throw ItemNotFoundException when item does not exist")
        void shouldThrowExceptionWhenItemNotFound() {
            Long itemId = 999L;
            when(itemDao.findById(itemId)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class, () -> itemService.getItemById(itemId));

            verify(itemDao, times(1)).findById(itemId);
            verifyNoInteractions(itemMapper);
        }
    }

    @Nested
    @DisplayName("Get All Items Tests")
    class GetAllItemsTests {

        @Test
        @DisplayName("Should return page of ItemDto when parameters are valid")
        void shouldReturnPageOfItemDtoWhenValid() {
            String itemName = "hammer";
            Pageable pageable = PageRequest.of(0, 10);

            Item item = Item.builder().id(1L).name("hammer").price(1000L).build();
            ItemDto itemDto = new ItemDto(1L, "hammer", 1000L);

            Page<Item> itemPage = new PageImpl<>(List.of(item), pageable, 1);


            when(itemDao.findAll(any(Specification.class), eq(pageable))).thenReturn(itemPage);
            when(itemMapper.toItemDto(item)).thenReturn(itemDto);

            Page<ItemDto> result = itemService.getAllItems(itemName, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("hammer", result.getContent().get(0).getName());

            verify(itemDao, times(1)).findAll(any(Specification.class), eq(pageable));
            verify(itemMapper, times(1)).toItemDto(any(Item.class));
        }

        @Test
        @DisplayName("Should throw ItemNullParameterException when pageable is null")
        void shouldThrowExceptionWhenPageableIsNull() {
            assertThrows(ItemNullParameterException.class, () -> itemService.getAllItems("hammer", null));

            verifyNoInteractions(itemDao, itemMapper);
        }
    }

    @Nested
    @DisplayName("Update Item Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("Should update item successfully")
        void shouldUpdateItemSuccessfully() {
            Long itemId = 1L;
            ItemDto itemDto = new ItemDto(itemId, "New Name", 1500L);

            Item existingItem = Item.builder()
                    .id(itemId)
                    .name("Old Name")
                    .price(1000L)
                    .build();

            Item updatedItem = Item.builder()
                    .id(itemId)
                    .name("New Name")
                    .price(1500L)
                    .build();

            when(itemDao.findById(itemId)).thenReturn(Optional.of(existingItem));
            doAnswer(invocation -> {
                ItemDto dto = invocation.getArgument(0);
                Item entity = invocation.getArgument(1);
                entity.setName(dto.getName());
                entity.setPrice(dto.getPrice());
                return null;
            }).when(itemMapper).updateItem(eq(itemDto), any(Item.class));

            when(itemDao.save(any(Item.class))).thenReturn(updatedItem);
            when(itemMapper.toItemDto(updatedItem)).thenReturn(itemDto);

            ItemDto result = itemService.updateItem(itemDto);

            assertNotNull(result);
            assertEquals("New Name", result.getName());

            verify(itemDao).findById(itemId);
            verify(itemMapper).updateItem(itemDto, existingItem);
            verify(itemDao).save(existingItem);
        }

        @Test
        @DisplayName("Should throw ItemNullParameterException when dto is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThrows(ItemNullParameterException.class, () -> itemService.updateItem(null));

            verifyNoInteractions(itemDao, itemMapper);
        }

        @Test
        @DisplayName("Should throw ItemNotFoundException when item does not exist")
        void shouldThrowExceptionWhenItemNotFound() {
            Long itemId = 1L;
            ItemDto itemDto = new ItemDto(itemId, "Name", 1000L);

            when(itemDao.findById(itemId)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class, () -> itemService.updateItem(itemDto));

            verify(itemDao).findById(itemId);
            verify(itemDao, never()).save(any());
            verify(itemMapper, never()).updateItem(any(), any());
        }
    }

    @Nested
    @DisplayName("Delete Item By Id Tests")
    class DeleteItemByIdTests {

        @Test
        @DisplayName("Should delete item successfully when id exists")
        void shouldDeleteItemSuccessfully() {
            Long itemId = 1L;
            Item item = Item.builder()
                    .id(itemId)
                    .name("Drill")
                    .price(2000L)
                    .build();

            when(itemDao.findById(itemId)).thenReturn(Optional.of(item));

            boolean result = itemService.deleteItemById(itemId);

            assertTrue(result);
            verify(itemDao, times(1)).findById(itemId);
            verify(itemDao, times(1)).delete(item);
        }

        @Test
        @DisplayName("Should throw ItemNullParameterException when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(ItemNullParameterException.class, () -> itemService.deleteItemById(null));

            verifyNoInteractions(itemDao, itemMapper);
        }

        @Test
        @DisplayName("Should throw ItemNotFoundException when item to delete not found")
        void shouldThrowExceptionWhenItemNotFound() {
            Long itemId = 999L;
            when(itemDao.findById(itemId)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class, () -> itemService.deleteItemById(itemId));

            verify(itemDao, times(1)).findById(itemId);
            verify(itemDao, never()).delete(any(Item.class));
        }
    }

}
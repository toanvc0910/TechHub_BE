# Manage Blogs — Single Sequence Diagram

This single diagram consolidates Manage Blogs flows in blog-service based on:
- controller/BlogController.java
- service/BlogService.java and service/impl/BlogServiceImpl.java

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as API Gateway
    participant BCtrl as BlogController
    participant BSvc as BlogServiceImpl
    participant BR as BlogRepository
    participant BM as BlogMapper
    participant NPub as NotificationCommandPublisher

    alt List blogs GET /api/blogs
        C->>G: GET .../blogs with keyword tags includeDrafts page size
        G->>BCtrl: GET /api/blogs
        BCtrl->>BSvc: getBlogs with filters and paging
        BSvc->>BSvc: normalize keyword and tags
        BSvc->>BSvc: determine privileged roles
        BSvc->>BSvc: determine has filters
        alt includeDrafts true and user privileged
            alt has filters
                BSvc->>BR: searchAll with keyword tags paging
            else no filters
                BSvc->>BR: findByIsActiveTrueOrderByCreatedDesc with paging
            end
        else public only
            alt has filters
                BSvc->>BR: searchPublished with status keyword tags paging
            else no filters
                BSvc->>BR: findByStatusAndIsActiveTrueOrderByCreatedDesc published with paging
            end
        end
        BR-->>BSvc: Page of Blog
        BSvc->>BM: map each to response
        BM-->>BSvc: Page of BlogResponse
        BSvc-->>BCtrl: Page of BlogResponse
        BCtrl-->>G: 200 OK PageGlobalResponse
        G-->>C: 200
    else Get blog by id GET /api/blogs/{blogId}
        C->>G: GET .../blogs/{blogId}
        G->>BCtrl: GET /api/blogs/{blogId}
        BCtrl->>BSvc: getBlog with blogId
        BSvc->>BR: findByIdAndIsActiveTrue
        BR-->>BSvc: Blog or not found
        alt blog not published
            BSvc->>BSvc: get current user id
            alt user is author
                BSvc-->>BCtrl: BlogResponse
                BCtrl-->>G: 200 OK GlobalResponse
                G-->>C: 200
            else not author
                BSvc-->>BCtrl: NotFound error
                BCtrl-->>G: 404 Not Found
                G-->>C: 404
            end
        else published
            BSvc-->>BCtrl: BlogResponse
            BCtrl-->>G: 200 OK GlobalResponse
            G-->>C: 200
        end
    else Create blog POST /api/blogs
        C->>G: POST .../blogs with BlogRequest
        G->>BCtrl: POST /api/blogs
        BCtrl->>BSvc: createBlog with request
        BSvc->>BSvc: require current user
        BSvc->>BM: toEntity from request
        BM-->>BSvc: Blog entity
        BSvc->>BSvc: set author id and createdBy and updatedBy
        BSvc->>BR: save blog
        BR-->>BSvc: Blog saved
        BSvc->>BSvc: notify publication if necessary
        BSvc->>BM: toResponse from blog
        BM-->>BSvc: BlogResponse
        BSvc-->>BCtrl: BlogResponse
        BCtrl-->>G: 201 Created GlobalResponse
        G-->>C: 201
    else Update blog PUT /api/blogs/{blogId}
        C->>G: PUT .../blogs/{blogId} with BlogRequest
        G->>BCtrl: PUT /api/blogs/{blogId}
        BCtrl->>BSvc: updateBlog with blogId and request
        BSvc->>BR: findByIdAndIsActiveTrue
        BR-->>BSvc: Blog
        BSvc->>BSvc: ensure can modify
        BSvc->>BSvc: record previous status
        BSvc->>BM: applyRequest to blog from request
        BM-->>BSvc: blog updated fields
        BSvc->>BSvc: set updatedBy
        BSvc->>BR: save blog
        BR-->>BSvc: Blog saved
        BSvc->>BSvc: notify publication if necessary
        BSvc->>BM: toResponse from blog
        BM-->>BSvc: BlogResponse
        BSvc-->>BCtrl: BlogResponse
        BCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    else Delete blog DELETE /api/blogs/{blogId}
        C->>G: DELETE .../blogs/{blogId}
        G->>BCtrl: DELETE /api/blogs/{blogId}
        BCtrl->>BSvc: deleteBlog with blogId
        BSvc->>BR: findByIdAndIsActiveTrue
        BR-->>BSvc: Blog
        BSvc->>BSvc: ensure can modify
        BSvc->>BSvc: set isActive false
        BSvc->>BSvc: set updatedBy
        BSvc->>BR: save blog
        BR-->>BSvc: Blog saved
        BSvc-->>BCtrl: void
        BCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    else Get blog tags GET /api/blogs/tags
        C->>G: GET .../blogs/tags
        G->>BCtrl: GET /api/blogs/tags
        BCtrl->>BSvc: getTags
        BSvc->>BR: findDistinctTags
        BR-->>BSvc: list of tag
        BSvc-->>BCtrl: list of tag
        BCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    end
```


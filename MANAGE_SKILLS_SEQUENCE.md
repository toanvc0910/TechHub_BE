# Manage Skills — Single Sequence Diagram

This single diagram consolidates Manage Skills flows in course-service based on:
- controller/SkillController.java
- service/SkillService.java and service/impl/SkillServiceImpl.java

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as API Gateway
    participant SCtrl as SkillController
    participant SSvc as SkillServiceImpl
    participant SR as SkillRepository

    alt Create skill POST /api/courses/skills
        C->>G: POST .../courses/skills with SkillDTO
        G->>SCtrl: POST /api/courses/skills
        SCtrl->>SSvc: createSkill(skillDTO)
        SSvc->>SSvc: map DTO to entity
        SSvc->>SR: save(new Skill)
        SR-->>SSvc: Skill
        SSvc-->>SCtrl: SkillDTO
        SCtrl-->>G: 201 Created response
        G-->>C: 201
    else Get skill by id GET /api/courses/skills/{id}
        C->>G: GET .../courses/skills/{id}
        G->>SCtrl: GET /api/courses/skills/{id}
        SCtrl->>SSvc: getSkill(id)
        SSvc->>SR: findById(id)
        SR-->>SSvc: Skill or null
        alt found
            SSvc-->>SCtrl: SkillDTO
            SCtrl-->>G: 200 OK response
            G-->>C: 200
        else not found
            SSvc-->>SCtrl: null
            SCtrl-->>G: 404 Not Found
            G-->>C: 404
        end
    else Get all skills GET /api/courses/skills
        C->>G: GET .../courses/skills
        G->>SCtrl: GET /api/courses/skills
        SCtrl->>SSvc: getAllSkills()
        SSvc->>SR: findAll()
        SR-->>SSvc: list of Skill
        SSvc-->>SCtrl: list of SkillDTO
        SCtrl-->>G: 200 OK page response
        G-->>C: 200
    else Update skill PUT /api/courses/skills/{id}
        C->>G: PUT .../courses/skills/{id} with SkillDTO
        G->>SCtrl: PUT /api/courses/skills/{id}
        SCtrl->>SSvc: updateSkill(id, skillDTO)
        SSvc->>SR: findById(id)
        SR-->>SSvc: Skill or null
        alt found
            SSvc->>SSvc: apply updates from DTO
            SSvc->>SR: save(updated Skill)
            SR-->>SSvc: Skill
            SSvc-->>SCtrl: SkillDTO
            SCtrl-->>G: 200 OK response
            G-->>C: 200
        else not found
            SSvc-->>SCtrl: null
            SCtrl-->>G: 404 Not Found
            G-->>C: 404
        end
    else Delete skill DELETE /api/courses/skills/{id}
        C->>G: DELETE .../courses/skills/{id}
        G->>SCtrl: DELETE /api/courses/skills/{id}
        SCtrl->>SSvc: deleteSkill(id)
        SSvc->>SR: deleteById(id)
        SSvc-->>SCtrl: void
        SCtrl-->>G: 200 OK response
        G-->>C: 200
    end
```


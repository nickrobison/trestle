import { IInterfacable } from "../../interfacable";

export interface ITrestleRelation {
    subject: string;
    object: string;
    relation: string;
}

export enum TrestleRelationType {
    // Spatial
    CONTAINS,
    COVERS,
    DISJOINT,
    EQUALS,
    INSIDE,
    MEETS,
    SPATIAL_OVERLAPS,
    // Temporal
    AFTER,
    BEFORE,
    BEGINS,
    DURING,
    ENDS,
    TEMPORAL_OVERLAPS,
    SPLIT_INTO,
    SPLIT_FROM,
    MERGED_INTO,
    MERGED_FROM
}

export class TrestleRelation implements IInterfacable<ITrestleRelation> {
    private subject: string;
    private object: string;
    private type: string;

    constructor(relation: ITrestleRelation) {
        this.subject = relation.subject;
        this.object = relation.object;
        this.type = relation.relation;
    }

    public getSubject(): string {
        return this.subject;
    }

    public getObject(): string {
        return this.object;
    }

    public getType(): string {
        return this.type;
    }

    public asInterface(): ITrestleRelation {
        return {
            subject: this.getSubject(),
            object: this.getObject(),
            relation: this.getType()
        };
    }
}

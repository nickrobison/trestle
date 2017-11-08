import {IInterfacable} from "../../interfacable";

export interface ITrestleRelation {
    subject: string;
    object: string;
    relation: string;
}

export type TrestleRelationType =
    "CONTAINS"
    | "COVERS"
    | "DISJOINT"
    | "EQUALS"
    | "INSIDE"
    | "MEETS"
    | "SPATIAL_OVERLAPS"
    | "AFTER"
    | "BEFORE"
    | "BEGINS"
    | "DURING"
    | "ENDS"
    | "TEMPORAL_OVERLAPS"
    | "SPLIT_INTO"
    | "SPLIT_FROM"
    | "MERGED_INTO"
    | "MERGED_FROM"
    | "COMPONENT_WITH";

// export enum TrestleRelationType {
//     // Spatial
//     CONTAINS,
//     COVERS,
//     DISJOINT,
//     EQUALS,
//     INSIDE,
//     MEETS,
//     SPATIAL_OVERLAPS,
//     // Temporal
//     AFTER,
//     BEFORE,
//     BEGINS,
//     DURING,
//     ENDS,
//     TEMPORAL_OVERLAPS,
//     SPLIT_INTO,
//     SPLIT_FROM,
//     MERGED_INTO,
//     MERGED_FROM
// }

export class TrestleRelation implements IInterfacable<ITrestleRelation> {
    private subject: string;
    private object: string;
    private type: TrestleRelationType;

    constructor(relation: ITrestleRelation) {
        this.subject = relation.subject;
        this.object = relation.object;
        this.type = (relation.relation as TrestleRelationType);
    }

    public getSubject(): string {
        return this.subject;
    }

    public getObject(): string {
        return this.object;
    }

    public getType(): TrestleRelationType {
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



module AbsCpp where

-- Haskell module generated by the BNF converter




newtype Id = Id String deriving (Eq, Ord, Show, Read)
data Program = PDefs [Def]
  deriving (Eq, Ord, Show, Read)

data Def
    = DFun Type Id [Arg] [Stm]
    | DFunEmpty Type Id [Arg]
    | DUsing QualifiedConstant
    | DTypeDef Type Id
    | DVariable Decl
  deriving (Eq, Ord, Show, Read)

data Arg
    = AFunc Type
    | ADecl Type Id
    | AConst Type Id
    | AConstType Type
    | AInitConst Type Id Exp
    | AInit Type Id Exp
  deriving (Eq, Ord, Show, Read)

data IDecl = DeclId Id | IDeclAss Id Exp
  deriving (Eq, Ord, Show, Read)

data Decl = ConstDecl Type [IDecl] | NoConstDecl Type [IDecl]
  deriving (Eq, Ord, Show, Read)

data Stm
    = STypeDef Type Id
    | SExp Exp
    | SDecl Decl
    | SReturn Exp
    | SWhile Exp Stm
    | SDoWhile Stm Exp
    | SBlock [Stm]
    | SIfElse Exp Stm Stm
    | SIf Exp Stm
    | SFor Decl Exp Exp Stm
    | SFuncConst Type Id Exp
  deriving (Eq, Ord, Show, Read)

data Exp
    = EConst QualifiedConstant
    | EString [String]
    | EInt Integer
    | EDouble Double
    | ETrue
    | EChar Char
    | EFalse
    | EIndex Exp Exp
    | EApp Exp [Exp]
    | Emem Exp Exp
    | EPIncr Exp
    | EPDecr Exp
    | EPointer Exp Exp
    | ENot Exp
    | EDRef Exp
    | EIncr Exp
    | EDecr Exp
    | ETimes Exp Exp
    | EDiv Exp Exp
    | EMod Exp Exp
    | EPlus Exp Exp
    | EMinus Exp Exp
    | ELshift Exp Exp
    | ERshift Exp Exp
    | ELt Exp Exp
    | EGt Exp Exp
    | ELtEq Exp Exp
    | EGtWq Exp Exp
    | EEq Exp Exp
    | ENEq Exp Exp
    | EAnd Exp Exp
    | EOr Exp Exp
    | EAss Exp Exp
    | EaDec Exp Exp
    | EaInc Exp Exp
    | EIfElse Exp Exp Exp
    | EThrow Exp
  deriving (Eq, Ord, Show, Read)

data Type
    = Tbool
    | Tdouble
    | TFloat
    | Tint
    | Tvoid
    | TChar
    | TList QualifiedConstant
    | TRef Type
  deriving (Eq, Ord, Show, Read)

data Types = TypeListElem Type
  deriving (Eq, Ord, Show, Read)

data QualifiedConstElem
    = QualifiedConstElemId Id | QualifiedConstElemInstance Id [Types]
  deriving (Eq, Ord, Show, Read)

data QualifiedConstant = QualifiedConstElems [QualifiedConstElem]
  deriving (Eq, Ord, Show, Read)


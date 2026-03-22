package exspecs

import com.microsoft.z3.*

fun makeProcs1() : List<Proc>  {
    val context = Context()
    val actIChan = createActionChannel(1)

    val enabledExpr1 = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(1)))
    val updateExpr = PlusIntUpdateExpr(IntVarUpdateExpr(VarName("i")), IntVarUpdateExpr(VarName("inc")))
    val updates1 = setOf(IntStateVarUpdate(VarName("i"), updateExpr))
    val actI1 = SymAction("I", listOf("inc"), enabledExpr1, updates1, actIChan)
    val initState = State(setOf(IntVarAssignment(VarName("i"), 0)))
    val p1 = Proc("p1", GenericTransitionSystem(setOf(actI1), initState))

    return listOf(p1)
}

fun makeProcs2() : List<Proc>  {
    val context = Context()
    val actIChan = createActionChannel(2)

    val enabledExpr1 = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(2)))
    val updateExpr1 = PlusIntUpdateExpr(IntVarUpdateExpr(VarName("i")), IntVarUpdateExpr(VarName("inc")))
    val updates1 = setOf(IntStateVarUpdate(VarName("i"), updateExpr1))
    val actI1 = SymAction("I", listOf("inc"), enabledExpr1, updates1, actIChan)
    val initState1 = State(setOf(IntVarAssignment(VarName("i"), 0)))
    val p1 = Proc("p1", GenericTransitionSystem(setOf(actI1), initState1))

    val enabledExpr2 = context.mkAnd(context.mkLe(context.mkIntConst("j"), context.mkInt(10)),
        context.mkEq(context.mkMod(context.mkIntConst("inc"), context.mkInt(2)), context.mkInt(0)))
    val updateExpr2 = PlusIntUpdateExpr(IntVarUpdateExpr(VarName("j")), IntVarUpdateExpr(VarName("inc")))
    val updates2 = setOf(IntStateVarUpdate(VarName("j"), updateExpr2))
    val actI2 = SymAction("I", listOf("inc"), enabledExpr2, updates2, actIChan)
    val initState2 = State(setOf(IntVarAssignment(VarName("j"), 0)))
    val p2 = Proc("p2", GenericTransitionSystem(setOf(actI2), initState2))

    return listOf(p1,p2)
}

fun runProcs(procs : List<Proc>) {
    println()
    val threads = procs.map { Thread(it) }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
}

fun main(args : Array<String>) {
    /*
    for (i in 1 .. 100) {
        runProcs(makeProcs1())
    }
    for (i in 1 .. 100) {
        runProcs(makeProcs2())
    }
     */
    runProcs(makeProcs2())
}

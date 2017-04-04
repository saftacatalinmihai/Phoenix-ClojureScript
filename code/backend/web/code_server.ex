defmodule Backend.CodeServer do
  @moduledoc false
  
  use GenServer

  def start_link do
    GenServer.start_link(__MODULE__, :ok, name: CodeServer)
  end

  def getActorTypes do
    GenServer.call(CodeServer, :get_actor_types)
  end

  def init(:ok) do
    actor_types = getActors() |> evalActors
    {:ok, %{:actor_types => actor_types}}
  end

  def handle_call(:get_actor_types, _from, state) do
    {:reply, state[:actor_types], state}
  end

  def handle_call(_msg, _from, state) do
    {:reply, :ok, state}
  end

  def handle_cast(_msg, state) do
    {:noreply, state}
  end

  defp getActors do
    Path.wildcard("code/*.ex") |>
    Enum.map(fn file ->
       String.replace_prefix(file, "code/", "") |>
       String.replace_suffix(".ex", "")
    end)
  end

  defp evalActors(actors) do
    actors |> Enum.map(&evalActor/1)
  end

  defp evalActor(type) do
    Code.eval_file("code/#{type}.ex")
    type
  end

end